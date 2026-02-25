# Krishi Sahayak RAG Backend

Production-ready Retrieval-Augmented Generation (RAG) backend for agricultural AI assistance.

## Tech Stack

- **Python 3.11**
- **FastAPI** - Modern web framework
- **LangChain** - RAG orchestration
- **FAISS** - Vector similarity search
- **HuggingFace Embeddings** - Text embeddings (all-MiniLM-L6-v2)
- **Ollama** - Local LLM provider (llama3:8b)

## Project Structure

```
krishi_rag/
├── app/
│   ├── __init__.py
│   ├── main.py           # FastAPI application entry point
│   ├── config.py         # Configuration management
│   ├── pdf_loader.py     # PDF ingestion and text extraction
│   ├── vector_store.py   # FAISS vector store operations
│   ├── retriever.py      # Document retrieval logic
│   ├── llm.py            # LLM integration
│   ├── rag_pipeline.py   # RAG pipeline orchestration
│   └── schemas.py        # Pydantic request/response models
├── data/
│   ├── pdfs/             # Source PDF documents
│   └── vector_store/     # FAISS index persistence
├── requirements.txt      # Python dependencies
├── .env.example          # Environment variables template
└── README.md             # This file
```

## Installation

1. **Clone the repository**
```bash
cd kisan-sahayak-ai/krishi_rag
```

2. **Create virtual environment**
```bash
python3.11 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. **Install dependencies**
```bash
pip install -r requirements.txt
```

4. **Configure environment variables**
```bash
cp .env.example .env
# Edit .env with your API keys and configuration
```

## Usage

### 1. Ingest PDFs

Place your agricultural PDF documents in `data/pdfs/` directory, then run:

```bash
curl -X POST http://localhost:8000/ingest
```

### 2. Start the server

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 3. Query the RAG system

```bash
curl -X POST http://localhost:8000/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What fertilizer is best for wheat in sandy soil?"}'
```

### 4. Health check

```bash
curl http://localhost:8000/health
```

## API Endpoints

### POST /ingest - Document Ingestion Pipeline

**Flow:**
```
1. Load document.docx from data/pdfs/
2. Split into chunks (800 chars, 150 overlap)
3. Detect section headings
4. Generate embeddings (HuggingFace all-MiniLM-L6-v2)
5. Store in FAISS vector store
```

**Request:**
```bash
curl -X POST http://localhost:8000/ingest
```

**Response:**
```json
{
  "status": "Vector store rebuilt successfully",
  "documents_indexed": 42
}
```

---

### POST /ask - Question Answering Pipeline

**Flow:**
```
1. Retrieve top 5 similar chunks from FAISS
2. Extract section names from chunks
3. Format context with sections
4. Send to LLM (GPT-3.5-turbo) with custom prompt
5. Return answer + source sections
```

**Request:**
```bash
curl -X POST http://localhost:8000/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the eligibility criteria?"}'
```

**Response:**
```json
{
  "answer": "According to the Eligibility Criteria section, farmers must have valid land records...",
  "sections": ["1. Eligibility Criteria", "2. Financial Benefits"],
  "success": true,
  "error": null
}
```

---

### GET /health - Health Check

```bash
curl http://localhost:8000/health
```

## How RAG Works in This System

1. **Ingestion**: PDFs are loaded and split into chunks (1000 chars, 200 overlap)
2. **Embedding**: Text chunks are converted to 768-dimensional vectors using HuggingFace
3. **Storage**: Vectors are stored in FAISS index for fast similarity search
4. **Retrieval**: User query is embedded and top-4 similar chunks are retrieved
5. **Generation**: Retrieved context + query are sent to LLM for answer generation
6. **Response**: Answer and source documents are returned to user

## Scaling to 10k PDFs

1. **Use batch processing** for embedding generation
2. **Implement pagination** for large result sets
3. **Add caching layer** (Redis) for frequent queries
4. **Use GPU** for faster embedding generation
5. **Shard FAISS index** across multiple files
6. **Add async processing** with Celery for ingestion
7. **Monitor memory usage** and implement cleanup strategies

## Replacing FAISS with Other Vector Stores

### Chroma
```python
from langchain.vectorstores import Chroma
vectorstore = Chroma.from_documents(documents, embeddings, persist_directory="./chroma_db")
```

### Pinecone
```python
from langchain.vectorstores import Pinecone
import pinecone
pinecone.init(api_key="your-key", environment="your-env")
vectorstore = Pinecone.from_documents(documents, embeddings, index_name="krishi")
```

## Making It Multilingual

1. **Use multilingual embeddings**: `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`
2. **Integrate translation API**: Bhashini for Indian languages
3. **Translate query** before retrieval
4. **Translate response** back to user's language
5. **Store documents** in multiple languages
6. **Use language-specific chunking** strategies

## Development

- Follow PEP 8 style guidelines
- Use type hints for all functions
- Write docstrings for all modules and functions
- Keep functions single-responsibility
- Use dependency injection where possible

## License

Proprietary - Indian Farmer Assistance Application
