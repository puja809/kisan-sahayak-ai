"""
Document retrieval logic using vector similarity search for Krishi Sahayak RAG backend.

This module provides retriever functionality to fetch relevant document chunks
based on semantic similarity to user queries.
"""

from typing import Optional

from langchain.schema.retriever import BaseRetriever

from app.vector_store import load_vector_store


def get_retriever() -> Optional[BaseRetriever]:
    """
    Create and return a retriever from the FAISS vector store.
    
    Loads the vector store from krishi_rag/vector_store/aif_index and
    configures it as a retriever with k=5 (returns top 5 most similar documents).
    
    The retriever performs semantic similarity search and returns documents
    with their full metadata (source, file_name, section).
    
    Returns:
        BaseRetriever: Configured retriever instance if vector store exists,
                      None if vector store hasn't been built yet.
        
    Raises:
        Exception: If retriever creation fails.
        
    Example:
        >>> retriever = get_retriever()
        >>> if retriever:
        >>>     docs = retriever.get_relevant_documents("What are eligibility criteria?")
        >>>     for doc in docs:
        >>>         print(doc.page_content)
        >>>         print(doc.metadata)  # Contains source, file_name, section
    """
    # Load the vector store
    vector_store = load_vector_store()
    
    if vector_store is None:
        print("✗ Cannot create retriever: Vector store not found.")
        print("  Please run build_vector_store() first to create the index.")
        return None
    
    # Create retriever with k=5 (return top 5 most similar documents)
    retriever = vector_store.as_retriever(
        search_type="similarity",
        search_kwargs={"k": 5}
    )
    
    print("✓ Retriever created successfully")
    print("  Configuration: search_type='similarity', k=5")
    
    return retriever
