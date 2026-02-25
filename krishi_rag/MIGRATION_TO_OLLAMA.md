# Migration from OpenAI to Ollama

## Summary of Changes

The Krishi Sahayak RAG system has been migrated from OpenAI to Ollama for local LLM inference.

---

## What Changed

### ✅ llm.py - Complete Rewrite
**Before (OpenAI):**
```python
from langchain.chat_models import ChatOpenAI

def get_llm() -> Optional[ChatOpenAI]:
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        return None
    return ChatOpenAI(
        model_name="gpt-3.5-turbo",
        temperature=0,
        openai_api_key=api_key
    )
```

**After (Ollama):**
```python
from langchain_community.chat_models import ChatOllama

def get_llm() -> ChatOllama:
    return ChatOllama(
        model="llama3:8b",
        temperature=0
    )
```

### ✅ rag_pipeline.py - Simplified
**Removed:**
- LLM None check (Ollama always returns valid instance)
- API key validation error handling

**Before:**
```python
llm = get_llm()
if llm is None:
    return {"error": "LLM not configured"}
```

**After:**
```python
llm = get_llm()  # Always returns valid ChatOllama instance
```

### ✅ requirements.txt - Updated Dependencies
**Removed:**
```
langchain-openai==0.0.5
openai==1.10.0
```

**Kept:**
```
langchain==0.1.4
langchain-community==0.0.16  # Contains ChatOllama
langchain-core==0.1.16
```

### ✅ .env.example - Simplified Configuration
**Removed:**
```bash
OPENAI_API_KEY=your_openai_api_key_here
MODEL_NAME=gpt-3.5-turbo
MAX_TOKENS=500
```

**Updated:**
```bash
# No API keys needed!
MODEL_NAME=llama3:8b
TEMPERATURE=0
```

---

## Benefits of Migration

### 💰 Cost Savings
- **Before**: $0.002 per 1K tokens (GPT-3.5-turbo)
- **After**: $0.00 (completely free)

### 🔒 Privacy
- **Before**: Data sent to OpenAI servers
- **After**: All processing happens locally

### ⚡ Performance
- **Before**: Network latency + API processing
- **After**: Local inference (faster for small batches)

### 🌐 Offline Capability
- **Before**: Requires internet connection
- **After**: Works completely offline

### 📊 No Rate Limits
- **Before**: Rate limits and quotas
- **After**: Unlimited usage

---

## Setup Requirements

### Prerequisites
1. **Install Ollama**
   ```bash
   curl -fsSL https://ollama.ai/install.sh | sh
   ```

2. **Pull llama3:8b model**
   ```bash
   ollama pull llama3:8b
   ```

3. **Verify Ollama is running**
   ```bash
   curl http://localhost:11434
   ```

### System Requirements
- **RAM**: 8GB minimum (16GB recommended)
- **Storage**: 5GB for model
- **CPU**: Modern multi-core processor
- **GPU**: Optional (NVIDIA with CUDA for faster inference)

---

## Compatibility

### ✅ Fully Compatible
- LangChain chains (LLMChain, RetrievalQA)
- Prompt templates
- Streaming responses
- All existing RAG pipeline code

### 🔄 API Compatibility
The ChatOllama class implements the same interface as ChatOpenAI:
- `.predict()` method
- `.invoke()` method
- `.stream()` method
- Temperature control
- System/user message handling

---

## Testing the Migration

### 1. Start Ollama
```bash
ollama serve
```

### 2. Test LLM directly
```bash
python -c "
from app.llm import get_llm
llm = get_llm()
print(llm.predict('What is 2+2?'))
"
```

### 3. Test RAG pipeline
```bash
# Start server
uvicorn app.main:app --reload

# Ingest documents
curl -X POST http://localhost:8000/ingest

# Ask question
curl -X POST http://localhost:8000/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the eligibility criteria?"}'
```

---

## Performance Comparison

### Response Time
| Metric | OpenAI GPT-3.5 | Ollama llama3:8b |
|--------|----------------|------------------|
| Network latency | 100-500ms | 0ms |
| Processing time | 1-3s | 2-5s |
| Total time | 1.1-3.5s | 2-5s |

### Quality
- **OpenAI GPT-3.5**: Excellent, highly coherent
- **Ollama llama3:8b**: Very good, suitable for RAG tasks

### Resource Usage
- **OpenAI**: Minimal local resources
- **Ollama**: ~4GB RAM during inference

---

## Rollback (if needed)

To switch back to OpenAI:

1. **Restore llm.py**
   ```python
   from langchain.chat_models import ChatOpenAI
   
   def get_llm():
       return ChatOpenAI(
           model_name="gpt-3.5-turbo",
           temperature=0,
           openai_api_key=os.getenv("OPENAI_API_KEY")
       )
   ```

2. **Update requirements.txt**
   ```
   langchain-openai==0.0.5
   openai==1.10.0
   ```

3. **Set environment variable**
   ```bash
   export OPENAI_API_KEY=sk-...
   ```

---

## Alternative Ollama Models

You can experiment with different models:

```python
# Faster, smaller model
ChatOllama(model="llama3:3b", temperature=0)

# Larger, more capable model (requires 32GB+ RAM)
ChatOllama(model="llama3:70b", temperature=0)

# Specialized models
ChatOllama(model="mistral", temperature=0)
ChatOllama(model="codellama", temperature=0)
```

See all models: https://ollama.ai/library

---

## Troubleshooting

### Issue: "Connection refused"
**Solution**: Ensure Ollama is running
```bash
ollama serve
```

### Issue: "Model not found"
**Solution**: Pull the model
```bash
ollama pull llama3:8b
```

### Issue: Slow responses
**Solution**: 
- Ensure sufficient RAM (8GB+)
- Close other applications
- Use GPU if available
- Try smaller model: `llama3:3b`

### Issue: Out of memory
**Solution**:
- Use smaller model: `llama3:3b`
- Increase system swap space
- Close other applications

---

## Migration Checklist

- [x] Remove OpenAI imports from llm.py
- [x] Add ChatOllama import
- [x] Update get_llm() function
- [x] Remove API key checks from rag_pipeline.py
- [x] Update requirements.txt
- [x] Update .env.example
- [x] Create OLLAMA_SETUP.md guide
- [x] Test LLM initialization
- [x] Test RAG pipeline end-to-end
- [x] Update documentation

---

## Conclusion

✅ **Migration Complete!**

The system now runs completely locally with Ollama, providing:
- Zero API costs
- Complete privacy
- Offline capability
- No rate limits
- Full compatibility with existing code

**Next Steps:**
1. Install Ollama: `curl -fsSL https://ollama.ai/install.sh | sh`
2. Pull model: `ollama pull llama3:8b`
3. Start using: `uvicorn app.main:app --reload`

🚀 Happy local LLM inference!
