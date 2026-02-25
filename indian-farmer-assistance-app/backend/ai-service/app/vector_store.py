"""Vector store adapter for MongoDB vector database."""
from datetime import datetime
from typing import Any, Dict, List, Optional

from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorCollection, AsyncIOMotorDatabase
from pymongo import ASCENDING, DESCENDING, IndexModel, TEXT

from app.config import settings
from app.logging_config import logger
from app.models import Document, DocumentMetadata, SearchFilters, SearchResult


class VectorStoreAdapter:
    """Adapter interface for vector database operations with MongoDB implementation."""

    def __init__(self, client: Optional[AsyncIOMotorClient] = None):
        """Initialize the vector store adapter."""
        self._client = client
        self._db: Optional[AsyncIOMotorDatabase] = None
        self._collection: Optional[AsyncIOMotorCollection] = None
        self._connected = False

    async def connect(self) -> None:
        """Establish connection to MongoDB."""
        try:
            if self._client is None:
                self._client = AsyncIOMotorClient(settings.mongodb_uri)
            
            self._db = self._client[settings.mongodb_database]
            self._collection = self._db[settings.mongodb_collection_documents]
            
            # Create indexes
            await self._create_indexes()
            
            self._connected = True
            logger.info("Connected to MongoDB successfully")
        except Exception as e:
            logger.error(f"Failed to connect to MongoDB: {e}")
            raise

    async def _create_indexes(self) -> None:
        """Create necessary indexes for the documents collection."""
        try:
            # Vector similarity search index (using ANN index for 768-dimensional vectors)
            # Note: MongoDB 5.0+ supports vector search indexes
            await self._collection.create_index(
                [("embedding", "knnVector", {"dimension": settings.vector_dimension, "similarity": "cosine"})],
                name="vector_embedding_idx"
            )
            
            # Regular indexes for filtering and querying
            await self._collection.create_index([("document_id", ASCENDING)], unique=True, name="document_id_idx")
            await self._collection.create_index([("category", ASCENDING)], name="category_idx")
            await self._collection.create_index([("metadata.state", ASCENDING)], name="state_idx")
            await self._collection.create_index([("metadata.tags", ASCENDING)], name="tags_idx")
            await self._collection.create_index([("metadata.applicable_crops", ASCENDING)], name="crops_idx")
            await self._collection.create_index([("is_active", ASCENDING)], name="is_active_idx")
            await self._collection.create_index([("created_at", DESCENDING)], name="created_at_idx")
            
            # Text index for full-text search fallback
            await self._collection.create_index(
                [("title", TEXT), ("content", TEXT)],
                name="text_search_idx"
            )
            
            logger.info("MongoDB indexes created successfully")
        except Exception as e:
            logger.warning(f"Could not create some indexes (may require MongoDB 5.0+): {e}")
            # Create basic indexes that work with older MongoDB versions
            await self._collection.create_index([("document_id", ASCENDING)], unique=True, name="document_id_idx")
            await self._collection.create_index([("category", ASCENDING)], name="category_idx")

    async def disconnect(self) -> None:
        """Close MongoDB connection."""
        if self._client:
            self._client.close()
            self._connected = False
            logger.info("Disconnected from MongoDB")

    async def is_connected(self) -> bool:
        """Check if connected to MongoDB."""
        return self._connected

    async def store_document(self, document: Document, embedding: List[float]) -> str:
        """Store a document with its embedding."""
        if not self._collection:
            raise RuntimeError("Not connected to MongoDB")
        
        doc_dict = document.model_dump()
        doc_dict["embedding"] = embedding
        doc_dict["updated_at"] = datetime.utcnow()
        
        if doc_dict.get("metadata"):
            if isinstance(doc_dict["metadata"], dict):
                doc_dict["metadata"]["upload_date"] = doc_dict["metadata"].get("upload_date") or datetime.utcnow()
        
        await self._collection.update_one(
            {"document_id": document.document_id},
            {"$set": doc_dict},
            upsert=True
        )
        
        logger.info(f"Stored document: {document.document_id}")
        return document.document_id

    async def search_by_similarity(
        self,
        query_embedding: List[float],
        limit: int = 10,
        filters: Optional[SearchFilters] = None
    ) -> List[SearchResult]:
        """Search documents by vector similarity."""
        if not self._collection:
            raise RuntimeError("Not connected to MongoDB")
        
        # Build filter query
        filter_query: Dict[str, Any] = {"is_active": True}
        
        if filters:
            if filters.category:
                filter_query["category"] = filters.category
            if filters.state:
                filter_query["metadata.state"] = filters.state
            if filters.tags:
                filter_query["metadata.tags"] = {"$in": filters.tags}
            if filters.applicable_crops:
                filter_query["metadata.applicable_crops"] = {"$in": filters.applicable_crops}
        
        try:
            # Use MongoDB vector search (requires MongoDB 5.0+)
            pipeline = [
                {"$match": filter_query},
                {
                    "$vectorSearch": {
                        "index": "vector_embedding_idx",
                        "path": "embedding",
                        "queryVector": query_embedding,
                        "numCandidates": limit * 10,
                        "limit": limit
                    }
                },
                {
                    "$project": {
                        "document_id": 1,
                        "title": 1,
                        "category": 1,
                        "content": 1,
                        "metadata": 1,
                        "similarityScore": {"$meta": "vectorSearchScore"}
                    }
                }
            ]
            
            cursor = self._collection.aggregate(pipeline)
            results = []
            
            async for doc in cursor:
                results.append(SearchResult(
                    document_id=doc["document_id"],
                    title=doc["title"],
                    category=doc["category"],
                    content=doc["content"][:500] if doc.get("content") else "",  # Truncate for response
                    similarity_score=doc.get("similarityScore", 0.0),
                    metadata=DocumentMetadata(**doc.get("metadata", {}))
                ))
            
            logger.info(f"Vector search returned {len(results)} results")
            return results
            
        except Exception as e:
            logger.warning(f"Vector search failed, falling back to text search: {e}")
            # Fallback to text search if vector search is not available
            return await self._text_search(query_embedding, limit, filter_query)

    async def _text_search(
        self,
        query_embedding: List[float],
        limit: int,
        filter_query: Dict[str, Any]
    ) -> List[SearchResult]:
        """Fallback text-based search when vector search is unavailable."""
        if not self._collection:
            raise RuntimeError("Not connected to MongoDB")
        
        # Get all matching documents and compute similarity manually
        cursor = self._collection.find(filter_query).limit(limit * 5)
        
        results = []
        async for doc in cursor:
            # Compute cosine similarity manually
            doc_embedding = doc.get("embedding", [])
            if doc_embedding and len(doc_embedding) == len(query_embedding):
                similarity = self._cosine_similarity(query_embedding, doc_embedding)
            else:
                similarity = 0.0
            
            results.append(SearchResult(
                document_id=doc["document_id"],
                title=doc["title"],
                category=doc["category"],
                content=doc.get("content", "")[:500],
                similarity_score=similarity,
                metadata=DocumentMetadata(**doc.get("metadata", {}))
            ))
        
        # Sort by similarity in descending order
        results.sort(key=lambda x: x.similarity_score, reverse=True)
        
        return results[:limit]

    def _cosine_similarity(self, vec1: List[float], vec2: List[float]) -> float:
        """Compute cosine similarity between two vectors."""
        import math
        
        if not vec1 or not vec2:
            return 0.0
        
        dot_product = sum(a * b for a, b in zip(vec1, vec2))
        norm1 = math.sqrt(sum(a * a for a in vec1))
        norm2 = math.sqrt(sum(b * b for b in vec2))
        
        if norm1 == 0 or norm2 == 0:
            return 0.0
        
        return dot_product / (norm1 * norm2)

    async def update_document(self, document_id: str, document: Document, embedding: List[float]) -> None:
        """Update a document and its embedding."""
        if not self._collection:
            raise RuntimeError("Not connected to MongoDB")
        
        doc_dict = document.model_dump()
        doc_dict["embedding"] = embedding
        doc_dict["updated_at"] = datetime.utcnow()
        
        # Increment version
        if "metadata" in doc_dict and isinstance(doc_dict["metadata"], dict):
            doc_dict["metadata"]["version"] = doc_dict["metadata"].get("version", 1) + 1
        
        await self._collection.update_one(
            {"document_id": document_id},
            {"$set": doc_dict}
        )
        
        logger.info(f"Updated document: {document_id}")

    async def delete_document(self, document_id: str) -> bool:
        """Soft-delete a document."""
        if not self._collection:
            raise RuntimeError("Not connected to MongoDB")
        
        result = await self._collection.update_one(
            {"document_id": document_id},
            {"$set": {"is_active": False, "updated_at": datetime.utcnow()}}
        )
        
        deleted = result.modified_count > 0
        if deleted:
            logger.info(f"Soft-deleted document: {document_id}")
        
        return deleted

    async def get_document(self, document_id: str) -> Optional[Document]:
        """Get a document by ID."""
        if not self._collection:
            raise RuntimeError("Not connected to MongoDB")
        
        doc = await self._collection.find_one({"document_id": document_id, "is_active": True})
        
        if doc:
            doc["_id"] = str(doc["_id"])
            return Document(**doc)
        
        return None

    async def list_documents(
        self,
        category: Optional[str] = None,
        state: Optional[str] = None,
        limit: int = 100,
        offset: int = 0
    ) -> List[Document]:
        """List documents with optional filtering."""
        if not self._collection:
            raise RuntimeError("Not connected to MongoDB")
        
        filter_query: Dict[str, Any] = {"is_active": True}
        
        if category:
            filter_query["category"] = category
        if state:
            filter_query["metadata.state"] = state
        
        cursor = self._collection.find(filter_query).skip(offset).limit(limit).sort("created_at", DESCENDING)
        
        documents = []
        async for doc in cursor:
            doc["_id"] = str(doc["_id"])
            documents.append(Document(**doc))
        
        return documents


# Global adapter instance
vector_store = VectorStoreAdapter()