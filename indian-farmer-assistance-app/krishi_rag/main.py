"""
FastAPI application entry point for Krishi Sahayak RAG backend.

This module provides REST API endpoints for document ingestion and
question-answering using the RAG pipeline.
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from app.pdf_loader import load_and_split_documents
from app.vector_store import build_vector_store
from app.rag_pipeline import query_rag_pipeline


# Initialize FastAPI app
app = FastAPI(
    title="Krishi Sahayak RAG API",
    description="Agriculture Infrastructure Fund (AIF) Scheme Assistant",
    version="1.0.0"
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Request/Response Models
class QuestionRequest(BaseModel):
    """Request model for asking questions."""
    question: str
    
    class Config:
        json_schema_extra = {
            "example": {
                "question": "What are the eligibility criteria for farmers?"
            }
        }


class QuestionResponse(BaseModel):
    """Response model for question answers."""
    answer: str
    sections: list[str]
    success: bool
    error: str | None = None
    
    class Config:
        json_schema_extra = {
            "example": {
                "answer": "According to the Eligibility Criteria section...",
                "sections": ["1. Eligibility Criteria", "2. Financial Benefits"],
                "success": True,
                "error": None
            }
        }


class IngestResponse(BaseModel):
    """Response model for document ingestion."""
    status: str
    documents_indexed: int
    
    class Config:
        json_schema_extra = {
            "example": {
                "status": "Vector store rebuilt successfully",
                "documents_indexed": 42
            }
        }


# API Endpoints
@app.get("/")
async def root():
    """Root endpoint with API information."""
    return {
        "message": "Krishi Sahayak RAG API",
        "description": "Agriculture Infrastructure Fund (AIF) Scheme Assistant",
        "endpoints": {
            "POST /ask": "Ask a question about the AIF scheme",
            "POST /ingest": "Rebuild vector store from document",
            "GET /health": "Check service health"
        }
    }


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {
        "status": "healthy",
        "service": "Krishi Sahayak RAG API",
        "version": "1.0.0"
    }


@app.post("/ingest", response_model=IngestResponse)
async def ingest_documents():
    """
    Ingest document and rebuild FAISS vector store.
    
    Loads the DOCX document from krishi_rag/data/pdfs/document.docx,
    splits it into chunks with section metadata, and rebuilds the
    FAISS vector store index.
    
    Returns:
        IngestResponse: Status and count of indexed documents.
        
    Raises:
        HTTPException: If document loading or indexing fails.
    """
    try:
        print("\n" + "="*60)
        print("📥 Starting document ingestion...")
        print("="*60)
        
        # Step 1: Load and split documents
        print("\n1️⃣ Loading document from: krishi_rag/data/pdfs/document.docx")
        documents = load_and_split_documents()
        
        if not documents:
            raise HTTPException(
                status_code=400,
                detail="No documents found or document is empty"
            )
        
        print(f"✓ Loaded and split into {len(documents)} chunks")
        
        # Step 2: Rebuild FAISS vector store
        print("\n2️⃣ Building FAISS vector store...")
        build_vector_store(documents)
        
        print("\n" + "="*60)
        print("✅ Ingestion completed successfully!")
        print("="*60 + "\n")
        
        return IngestResponse(
            status="Vector store rebuilt successfully",
            documents_indexed=len(documents)
        )
        
    except FileNotFoundError as e:
        raise HTTPException(
            status_code=404,
            detail=f"Document not found: {str(e)}"
        )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error during ingestion: {str(e)}"
        )


@app.post("/ask", response_model=QuestionResponse)
async def ask_question(request: QuestionRequest):
    """
    Ask a question about the AIF scheme.
    
    Retrieves relevant document chunks and generates an answer
    using the RAG pipeline with context from the AIF documentation.
    
    Args:
        request: QuestionRequest with the user's question.
        
    Returns:
        QuestionResponse: Generated answer with source sections.
        
    Raises:
        HTTPException: If question processing fails.
    """
    try:
        if not request.question or not request.question.strip():
            raise HTTPException(
                status_code=400,
                detail="Question cannot be empty"
            )
        
        print("\n" + "="*60)
        print(f"❓ Question: {request.question}")
        print("="*60)
        
        # Execute RAG pipeline
        result = query_rag_pipeline(request.question)
        
        print("="*60 + "\n")
        
        return QuestionResponse(**result)
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error processing question: {str(e)}"
        )


# Run with: uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
