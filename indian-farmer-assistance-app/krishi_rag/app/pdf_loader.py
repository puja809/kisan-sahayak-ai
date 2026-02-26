"""
Document loading and text extraction utilities for Krishi Sahayak RAG backend.

This module handles loading DOCX documents and splitting them into chunks
for vector embedding and retrieval.
"""

import re
from pathlib import Path
from typing import List, Optional

from langchain_community.document_loaders import Docx2txtLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.schema import Document


def _detect_section_heading(text: str) -> Optional[str]:
    """
    Detect if a line is a section heading.
    
    Detects headings that:
    - Start with numbers like "1.", "2.", "3.", etc.
    - Contain specific keywords like "Eligibility", "Financial Benefits", etc.
    
    Args:
        text: Text line to check for heading patterns.
        
    Returns:
        The detected heading text if found, None otherwise.
    """
    # Strip whitespace for consistent matching
    line = text.strip()
    
    if not line:
        return None
    
    # Pattern 1: Lines starting with numbers like "1.", "2.", "3."
    numbered_pattern = r'^\d+\.\s*(.+)$'
    match = re.match(numbered_pattern, line)
    if match:
        return line
    
    # Pattern 2: Lines containing specific keywords
    keywords = [
        "Eligibility",
        "Financial Benefits",
        "FAQs",
        "Application Process",
        "Monitoring"
    ]
    
    for keyword in keywords:
        if keyword.lower() in line.lower():
            return line
    
    return None


def _extract_sections_from_document(document_text: str) -> List[tuple]:
    """
    Extract sections from document text by detecting headings.
    
    Args:
        document_text: Full document text.
        
    Returns:
        List of tuples (section_name, section_text) for each detected section.
    """
    lines = document_text.split('\n')
    sections = []
    current_section = "Introduction"  # Default section for content before first heading
    current_text = []
    
    for line in lines:
        detected_heading = _detect_section_heading(line)
        
        if detected_heading:
            # Save previous section if it has content
            if current_text:
                sections.append((current_section, '\n'.join(current_text)))
            
            # Start new section
            current_section = detected_heading
            current_text = [line]  # Include heading in section text
        else:
            current_text.append(line)
    
    # Add final section
    if current_text:
        sections.append((current_section, '\n'.join(current_text)))
    
    return sections


def load_and_split_documents() -> List[Document]:
    """
    Load DOCX document and split into chunks with section metadata.
    
    Loads the document from krishi_rag/data/pdfs/document.docx relative to
    the project root, detects section headings, splits into chunks, and adds
    metadata including section information to each chunk.
    
    Section headings are detected by:
    - Lines starting with numbers (e.g., "1.", "2.", "3.")
    - Lines containing keywords: Eligibility, Financial Benefits, FAQs,
      Application Process, Monitoring
    
    Returns:
        List[Document]: List of document chunks with metadata containing:
            - source: "AIF_document"
            - file_name: "document.docx"
            - section: Detected section heading
        
    Raises:
        FileNotFoundError: If the document file doesn't exist.
        Exception: If document loading or splitting fails.
    """
    # Get project root directory (krishi_rag/)
    # This works whether running from project root or app/ directory
    current_file = Path(__file__).resolve()
    project_root = current_file.parent.parent  # Go up from app/ to krishi_rag/
    
    # Construct path to document
    doc_path = project_root / "data" / "pdfs" / "document.docx"
    
    # Verify file exists
    if not doc_path.exists():
        raise FileNotFoundError(
            f"Document not found at: {doc_path}\n"
            f"Please ensure document.docx exists in data/pdfs/ directory."
        )
    
    # Load document using Docx2txtLoader
    loader = Docx2txtLoader(str(doc_path))
    documents = loader.load()
    
    # Extract full document text
    full_text = documents[0].page_content if documents else ""
    
    # Extract sections from document
    sections = _extract_sections_from_document(full_text)
    
    # Configure text splitter
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=800,
        chunk_overlap=150,
        length_function=len,
        separators=["\n\n", "\n", " ", ""]
    )
    
    # Process each section and create chunks
    all_chunks = []
    
    for section_name, section_text in sections:
        # Create a document for this section
        section_doc = Document(
            page_content=section_text,
            metadata={"section": section_name}
        )
        
        # Split section into chunks
        section_chunks = text_splitter.split_documents([section_doc])
        
        # Add base metadata to every chunk
        for chunk in section_chunks:
            chunk.metadata.update({
                "source": "AIF_document",
                "file_name": "document.docx",
                "section": section_name  # Ensure section is preserved
            })
        
        all_chunks.extend(section_chunks)
    
    return all_chunks
