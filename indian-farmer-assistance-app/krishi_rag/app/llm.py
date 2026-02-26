"""
LLM integration and configuration for Krishi Sahayak RAG backend.

This module handles Ollama LLM initialization and configuration
for answer generation in the RAG pipeline.
"""

from langchain_community.chat_models import ChatOllama


def get_llm() -> ChatOllama:
    """
    Initialize and return Ollama LLM instance.
    
    Creates a ChatOllama instance configured for RAG answer generation
    using the llama3:8b model running locally.
    
    Configuration:
    - Model: llama3:8b (local Ollama model)
    - Temperature: 0 (deterministic, factual answers)
    - Base URL: http://localhost:11434 (default Ollama endpoint)
    
    Returns:
        ChatOllama: Configured LLM instance ready for use.
        
    Requirements:
        - Ollama must be installed and running locally
        - llama3:8b model must be pulled: `ollama pull llama3:8b`
        
    Example:
        >>> llm = get_llm()
        >>> response = llm.predict("What is the capital of France?")
        >>> print(response)
        
    Note:
        No API keys required. Ollama runs locally on your machine.
        Compatible with LangChain's RetrievalQA and other chains.
    """
    # Initialize ChatOllama with llama3:8b model
    llm = ChatOllama(
        model="gemma3:1b",
        temperature=0
    )
    
    print("✓ LLM initialized: llama3:8b (Ollama, temp=0)")
    print("  Ensure Ollama is running: http://localhost:11434")
    
    return llm
