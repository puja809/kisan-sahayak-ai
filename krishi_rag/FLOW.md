# Krishi Sahayak RAG - Complete Flow

## POST /ingest - Document Ingestion Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                         POST /ingest                              │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 1: Load Document                                             │
│ ─────────────────────────────────────────────────────────────    │
│ • File: krishi_rag/data/pdfs/document.docx                       │
│ • Loader: Docx2txtLoader (LangChain)                             │
│ • Output: Full document text                                     │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 2: Detect Section Headings                                  │
│ ─────────────────────────────────────────────────────────────    │
│ • Pattern 1: Lines starting with "1.", "2.", "3."                │
│ • Pattern 2: Keywords (Eligibility, Financial Benefits, etc.)    │
│ • Output: Sections with boundaries                               │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 3: Split into Chunks                                        │
│ ─────────────────────────────────────────────────────────────    │
│ • Splitter: RecursiveCharacterTextSplitter                       │
│ • Chunk size: 800 characters                                     │
│ • Overlap: 150 characters                                        │
│ • Separators: ["\n\n", "\n", " ", ""]                           │
│ • Output: List of Document chunks                                │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 4: Add Metadata to Each Chunk                               │
│ ─────────────────────────────────────────────────────────────    │
│ • source: "AIF_document"                                         │
│ • file_name: "document.docx"                                     │
│ • section: "1. Eligibility Criteria" (detected heading)          │
│ • Output: Chunks with complete metadata                          │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 5: Generate Embeddings                                      │
│ ─────────────────────────────────────────────────────────────    │
│ • Model: sentence-transformers/all-MiniLM-L6-v2                 │
│ • Dimensions: 384                                                │
│ • Normalization: Enabled                                         │
│ • Output: Vector embeddings for each chunk                       │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 6: Build FAISS Index                                        │
│ ─────────────────────────────────────────────────────────────    │
│ • Vector Store: FAISS                                            │
│ • Index Type: Flat (exact search)                               │
│ • Metadata: Stored with vectors                                  │
│ • Output: FAISS index with metadata                              │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 7: Save to Disk                                             │
│ ─────────────────────────────────────────────────────────────    │
│ • Path: krishi_rag/vector_store/aif_index/                      │
│ • Files: index.faiss, index.pkl                                  │
│ • Persistence: Reusable across restarts                          │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Response                                                          │
│ ─────────────────────────────────────────────────────────────    │
│ {                                                                │
│   "status": "Vector store rebuilt successfully",                │
│   "documents_indexed": 42                                        │
│ }                                                                │
└──────────────────────────────────────────────────────────────────┘
```

---

## POST /ask - Question Answering Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                          POST /ask                                │
│ Request: {"question": "What are the eligibility criteria?"}      │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 1: Load Vector Store                                        │
│ ─────────────────────────────────────────────────────────────    │
│ • Path: krishi_rag/vector_store/aif_index/                      │
│ • Load: FAISS index + metadata                                   │
│ • Output: Vector store instance                                  │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 2: Create Retriever                                         │
│ ─────────────────────────────────────────────────────────────    │
│ • Search type: similarity                                        │
│ • k: 5 (top 5 results)                                          │
│ • Output: Configured retriever                                   │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 3: Embed Query                                              │
│ ─────────────────────────────────────────────────────────────    │
│ • Query: "What are the eligibility criteria?"                   │
│ • Model: sentence-transformers/all-MiniLM-L6-v2                 │
│ • Output: Query vector (384 dimensions)                          │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 4: Similarity Search                                        │
│ ─────────────────────────────────────────────────────────────    │
│ • Algorithm: Cosine similarity                                   │
│ • Search: Top 5 most similar chunks                             │
│ • Output: 5 Document objects with metadata                       │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 5: Extract Sections                                         │
│ ─────────────────────────────────────────────────────────────    │
│ • Read metadata["section"] from each chunk                       │
│ • Remove duplicates                                              │
│ • Output: ["1. Eligibility Criteria", "2. Financial Benefits"]  │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 6: Format Context                                           │
│ ─────────────────────────────────────────────────────────────    │
│ • Format:                                                        │
│   [Section: 1. Eligibility Criteria]                            │
│   Farmers must have valid land records...                       │
│                                                                  │
│   [Section: 2. Financial Benefits]                              │
│   The scheme provides up to ₹50,000...                          │
│ • Output: Formatted context string                               │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 7: Create Prompt                                            │
│ ─────────────────────────────────────────────────────────────    │
│ • System: "You are an expert assistant for AIF scheme..."       │
│ • Context: Formatted chunks with sections                        │
│ • Question: User's question                                      │
│ • Instructions: Answer only from context, mention sections       │
│ • Output: Complete prompt                                        │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 8: Send to LLM                                              │
│ ─────────────────────────────────────────────────────────────    │
│ • Model: gpt-3.5-turbo                                           │
│ • Temperature: 0 (deterministic)                                 │
│ • Max tokens: 500                                                │
│ • Output: Generated answer                                       │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Step 9: Structure Response                                       │
│ ─────────────────────────────────────────────────────────────    │
│ • answer: LLM-generated answer                                   │
│ • sections: Extracted section names                              │
│ • success: true                                                  │
│ • error: null                                                    │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ Response                                                          │
│ ─────────────────────────────────────────────────────────────    │
│ {                                                                │
│   "answer": "According to the Eligibility Criteria section,     │
│              farmers must have valid land records and be         │
│              registered with the local agriculture department.", │
│   "sections": ["1. Eligibility Criteria"],                      │
│   "success": true,                                               │
│   "error": null                                                  │
│ }                                                                │
└──────────────────────────────────────────────────────────────────┘
```

---

## Data Flow Summary

### Ingestion Pipeline
```
document.docx → Load → Detect Sections → Split → Add Metadata → 
Embed → FAISS Index → Save to Disk
```

### Query Pipeline
```
Question → Embed → Similarity Search → Retrieve Top 5 → 
Extract Sections → Format Context → LLM → Answer + Sections
```

---

## Module Responsibilities

| Module | Responsibility |
|--------|---------------|
| `pdf_loader.py` | Load DOCX, detect sections, split into chunks |
| `vector_store.py` | Build/load FAISS index, manage embeddings |
| `retriever.py` | Configure retriever, perform similarity search |
| `llm.py` | Initialize OpenAI LLM |
| `rag_pipeline.py` | Orchestrate retrieval + generation |
| `main.py` | FastAPI endpoints, request/response handling |

---

## Key Design Decisions

✅ **Section Detection**: Automatic heading detection for better context  
✅ **Metadata Preservation**: Source tracking through entire pipeline  
✅ **Chunk Overlap**: 150 chars to maintain context across boundaries  
✅ **Top-k Retrieval**: 5 chunks for comprehensive context  
✅ **Temperature 0**: Deterministic, factual answers  
✅ **Custom Prompt**: Strict adherence to provided context  
✅ **Structured Response**: JSON with answer and source sections  
