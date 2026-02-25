"""
RAG pipeline orchestration combining retrieval and generation for Krishi Sahayak.

This module implements the complete RAG workflow: retrieve relevant documents
and generate answers using LLM with custom prompts.
"""

from typing import Dict, List, Optional

from langchain.prompts import PromptTemplate
from langchain.schema import Document
from langchain.chains import LLMChain

from app.retriever import get_retriever
from app.llm import get_llm


# Custom prompt template for AIF scheme assistant
PROMPT_TEMPLATE = """You are an expert assistant for the Agriculture Infrastructure Fund (AIF) scheme.
Answer ONLY from the provided context.
If the answer is not present in the context, say: 'I could not find this information in the AIF documentation.'
Always mention the section name in the answer.

Context:
{context}

Question: {question}

Answer:"""


def _format_context(documents: List[Document]) -> str:
    """
    Format retrieved documents into context string.
    
    Args:
        documents: List of retrieved Document objects with metadata.
        
    Returns:
        Formatted context string with section information.
    """
    context_parts = []
    
    for i, doc in enumerate(documents, 1):
        section = doc.metadata.get("section", "Unknown Section")
        content = doc.page_content.strip()
        
        context_parts.append(f"[Section: {section}]\n{content}")
    
    return "\n\n".join(context_parts)


def _extract_sections(documents: List[Document]) -> List[str]:
    """
    Extract unique section names from retrieved documents.
    
    Args:
        documents: List of retrieved Document objects with metadata.
        
    Returns:
        List of unique section names.
    """
    sections = []
    seen = set()
    
    for doc in documents:
        section = doc.metadata.get("section", "Unknown Section")
        if section not in seen:
            sections.append(section)
            seen.add(section)
    
    return sections


def query_rag_pipeline(question: str) -> Dict[str, any]:
    """
    Execute RAG pipeline to answer a question.
    
    Retrieves relevant documents from the vector store and generates
    an answer using the LLM with custom AIF scheme prompt.
    
    Args:
        question: User's question about the AIF scheme.
        
    Returns:
        Dictionary with structured response:
        {
            "answer": "Generated answer from LLM",
            "sections": ["Section Name 1", "Section Name 2"],
            "success": True/False,
            "error": "Error message if failed" (optional)
        }
        
    Example:
        >>> result = query_rag_pipeline("What are the eligibility criteria?")
        >>> print(result["answer"])
        >>> print(result["sections"])
    """
    try:
        # Step 1: Get retriever
        retriever = get_retriever()
        
        if retriever is None:
            return {
                "answer": "Vector store not initialized. Please build the index first.",
                "sections": [],
                "success": False,
                "error": "Vector store not found"
            }
        
        # Step 2: Retrieve relevant documents
        print(f"\n🔍 Retrieving relevant documents for: '{question}'")
        documents = retriever.get_relevant_documents(question)
        
        if not documents:
            return {
                "answer": "I could not find this information in the AIF documentation.",
                "sections": [],
                "success": True
            }
        
        print(f"✓ Retrieved {len(documents)} relevant document chunks")
        
        # Step 3: Extract sections from retrieved documents
        sections = _extract_sections(documents)
        print(f"✓ Relevant sections: {', '.join(sections)}")
        
        # Step 4: Format context from documents
        context = _format_context(documents)
        
        # Step 5: Get LLM
        llm = get_llm()
        
        # Step 6: Create prompt template
        prompt = PromptTemplate(
            template=PROMPT_TEMPLATE,
            input_variables=["context", "question"]
        )
        
        # Step 7: Create LLM chain
        chain = LLMChain(llm=llm, prompt=prompt)
        
        # Step 8: Generate answer
        print("🤖 Generating answer...")
        response = chain.run(context=context, question=question)
        
        answer = response.strip()
        
        print("✓ Answer generated successfully\n")
        
        # Step 9: Return structured response
        return {
            "answer": answer,
            "sections": sections,
            "success": True
        }
        
    except Exception as e:
        print(f"✗ Error in RAG pipeline: {e}")
        return {
            "answer": f"An error occurred while processing your question: {str(e)}",
            "sections": [],
            "success": False,
            "error": str(e)
        }
