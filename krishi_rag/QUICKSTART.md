# Krishi Sahayak RAG - Quick Start Guide

## Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    POST /ingest                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Load document.docx                                       │
│     ↓                                                        │
│  2. Split into chunks (800 chars, 150 overlap)              │
│     ↓                                                        │
│  3. Detect section headings                                 │
│     ↓                                                        │
│  4. Generate embeddings (HuggingFace all-MiniLM-L6-v2)      │
│     ↓                                                        │
│  5. Store in FAISS (vector_store/aif_index/)                │
│                                                              │
│  Response: {"status": "success", "documents_indexed": 42}   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     POST /ask                                │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Receive question                                         │
│     ↓                                                        │
│  2. Retrieve top 5 similar chunks from FAISS                │
│     ↓                                                        │
│  3. Extract section names from metadata                     │
│     ↓                                                        │
│  4. Format context with sections                            │
│     ↓                                                        │
│  5. Send to LLM (GPT-3.5-turbo) with custom prompt         │
│     ↓                                                        │
│  6. Return answer + source sections                         │
│                                                              │
│  Response: {"answer": "...", "sections": [...]}             │
└─────────────────────────────────────────────────────────────┘
```

## Setup (5 minutes)

### 1. Install Dependencies
```bash
cd kisan-sahayak-ai/krishi_rag
python3.11 -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 2. Configure Environment
```bash
cp .env.example .env
# Edit .env and add your OpenAI API key:
# OPENAI_API_KEY=sk-...
```

### 3. Place Your Document
```bash
# Put your document.docx in:
# krishi_rag/data/pdfs/document.docx
```

## Usage

### Start Server
```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### Ingest Document (First Time)
```bash
curl -X POST http://localhost:8000/ingest
```

**Output:**
```
📥 Starting document ingestion...
1️⃣ Loading document from: krishi_rag/data/pdfs/document.docx
✓ Loaded and split into 42 chunks
2️⃣ Building FAISS vector store...
✓ Vector store built and saved
✅ Ingestion completed successfully!
```

### Ask Questions
```bash
curl -X POST http://localhost:8000/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the eligibility criteria for farmers?"}'
```

**Output:**
```json
{
  "answer": "According to the Eligibility Criteria section, farmers must have valid land records and be registered with the local agriculture department. The scheme is available for individual farmers, FPOs, and cooperatives.",
  "sections": ["1. Eligibility Criteria"],
  "success": true
}
```

## Example Questions

```bash
# Eligibility
curl -X POST http://localhost:8000/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Who is eligible for the AIF scheme?"}'

# Financial Benefits
curl -X POST http://localhost:8000/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What financial benefits are provided?"}'

# Application Process
curl -X POST http://localhost:8000/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "How do I apply for the scheme?"}'

# FAQs
curl -X POST http://localhost:8000/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What documents are required for application?"}'
```

## Interactive API Documentation

Visit these URLs after starting the server:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## Troubleshooting

### Vector store not found
```bash
# Solution: Run ingestion first
curl -X POST http://localhost:8000/ingest
```

### OpenAI API key error
```bash
# Solution: Set OPENAI_API_KEY in .env file
echo "OPENAI_API_KEY=sk-your-key-here" >> .env
```

### Document not found
```bash
# Solution: Ensure document.docx exists
ls data/pdfs/document.docx
```

## Architecture

```
app/
├── main.py           # FastAPI endpoints
├── pdf_loader.py     # Document loading & splitting
├── vector_store.py   # FAISS operations
├── retriever.py      # Document retrieval
├── llm.py            # OpenAI LLM
├── rag_pipeline.py   # RAG orchestration
└── schemas.py        # Pydantic models

data/
├── pdfs/
│   └── document.docx # Source document

vector_store/
└── aif_index/        # FAISS index (auto-generated)
```

## Key Features

✅ **Section-Aware Retrieval**: Automatically detects and tracks document sections  
✅ **Metadata Preservation**: Every chunk includes source, file_name, and section  
✅ **Smart Chunking**: 800 chars with 150 overlap for context preservation  
✅ **Custom Prompts**: Tailored for AIF scheme with strict context adherence  
✅ **Structured Responses**: JSON with answer and source sections  
✅ **Production Ready**: Error handling, logging, CORS, health checks  

## Next Steps

1. ✅ Place your document.docx in data/pdfs/
2. ✅ Set OPENAI_API_KEY in .env
3. ✅ Run: `uvicorn app.main:app --reload`
4. ✅ Ingest: `curl -X POST http://localhost:8000/ingest`
5. ✅ Ask: `curl -X POST http://localhost:8000/ask -H "Content-Type: application/json" -d '{"question": "..."}'`

Happy querying! 🚀
