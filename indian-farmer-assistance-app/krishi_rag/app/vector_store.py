"""
FAISS vector store management and operations for Krishi Sahayak RAG backend.

This module handles creating, saving, and loading FAISS vector stores with
HuggingFace embeddings for document retrieval.
"""

from pathlib import Path
from typing import List, Optional

from langchain.embeddings import HuggingFaceEmbeddings
from langchain.vectorstores import FAISS
from langchain.schema import Document


def _get_embeddings() -> HuggingFaceEmbeddings:
    """
    Initialize and return HuggingFace embeddings model.
    
    Uses sentence-transformers/all-MiniLM-L6-v2 model for generating
    384-dimensional embeddings.
    
    Returns:
        HuggingFaceEmbeddings: Configured embeddings model.
    """
    embeddings = HuggingFaceEmbeddings(
        model_name="sentence-transformers/all-MiniLM-L6-v2",
        model_kwargs={'device': 'cpu'},  # Use 'cuda' for GPU
        encode_kwargs={'normalize_embeddings': True}
    )
    return embeddings


def _get_vector_store_path() -> Path:
    """
    Get the path to the FAISS vector store directory.
    
    Returns:
        Path: Absolute path to krishi_rag/vector_store/aif_index/
    """
    # Get project root directory (krishi_rag/)
    current_file = Path(__file__).resolve()
    project_root = current_file.parent.parent  # Go up from app/ to krishi_rag/
    
    # Construct path to vector store
    vector_store_path = project_root / "vector_store" / "aif_index"
    
    return vector_store_path


def build_vector_store(documents: List[Document]) -> FAISS:
    """
    Build FAISS vector store from documents and save to disk.
    
    Creates embeddings for all documents using HuggingFace embeddings,
    builds a FAISS index, and persists it to disk. Metadata from documents
    is automatically stored with the embeddings.
    
    Args:
        documents: List of Document objects with page_content and metadata.
        
    Returns:
        FAISS: The built vector store instance.
        
    Raises:
        ValueError: If documents list is empty.
        Exception: If vector store creation or saving fails.
    """
    if not documents:
        raise ValueError("Cannot build vector store from empty documents list.")
    
    # Initialize embeddings model
    embeddings = _get_embeddings()
    
    # Build FAISS vector store from documents
    # Metadata is automatically stored with each embedding
    vector_store = FAISS.from_documents(
        documents=documents,
        embedding=embeddings
    )
    
    # Get save path
    save_path = _get_vector_store_path()
    
    # Create directory if it doesn't exist
    save_path.parent.mkdir(parents=True, exist_ok=True)
    
    # Save vector store to disk
    vector_store.save_local(str(save_path))
    
    print(f"✓ Vector store built and saved to: {save_path}")
    print(f"✓ Total documents indexed: {len(documents)}")
    
    return vector_store


def load_vector_store() -> Optional[FAISS]:
    """
    Load existing FAISS vector store from disk.
    
    Attempts to load a previously saved FAISS index from the configured
    path. If the index doesn't exist, returns None.
    
    Returns:
        FAISS: Loaded vector store instance if exists, None otherwise.
        
    Raises:
        Exception: If loading fails for reasons other than file not found.
    """
    # Get load path
    load_path = _get_vector_store_path()
    
    # Check if vector store exists
    if not load_path.exists():
        print(f"✗ Vector store not found at: {load_path}")
        print(f"  Run build_vector_store() first to create the index.")
        return None
    
    # Initialize embeddings model (same as used for building)
    embeddings = _get_embeddings()
    
    try:
        # Load vector store from disk
        vector_store = FAISS.load_local(
            str(load_path),
            embeddings,
            allow_dangerous_deserialization=True  # Required for FAISS loading
        )
        
        print(f"✓ Vector store loaded from: {load_path}")
        
        return vector_store
        
    except Exception as e:
        print(f"✗ Error loading vector store: {e}")
        raise
