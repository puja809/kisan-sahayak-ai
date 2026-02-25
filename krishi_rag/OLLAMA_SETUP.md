# Ollama Setup Guide for Krishi Sahayak RAG

## What is Ollama?

Ollama allows you to run large language models locally on your machine. No API keys, no cloud costs, complete privacy!

## Installation

### macOS / Linux
```bash
curl -fsSL https://ollama.ai/install.sh | sh
```

### Windows
Download and install from: https://ollama.ai

### Verify Installation
```bash
ollama --version
```

## Pull llama3:8b Model

```bash
# Pull the model (one-time, ~4.7GB download)
ollama pull llama3:8b

# Verify model is available
ollama list
```

## Start Ollama Server

Ollama runs as a background service by default after installation.

### Check if Ollama is running
```bash
curl http://localhost:11434
```

**Expected response:**
```
Ollama is running
```

### Manually start Ollama (if needed)
```bash
ollama serve
```

## Test Ollama

```bash
# Test with a simple prompt
ollama run llama3:8b "What is the capital of France?"
```

## Integration with Krishi Sahayak

The RAG system is already configured to use Ollama!

### Configuration in llm.py
```python
from langchain_community.chat_models import ChatOllama

def get_llm():
    return ChatOllama(
        model="llama3:8b",
        temperature=0
    )
```

### No Environment Variables Needed!
- ❌ No OPENAI_API_KEY
- ❌ No cloud API setup
- ✅ Just install Ollama and pull the model

## Usage in RAG System

```bash
# 1. Ensure Ollama is running
curl http://localhost:11434

# 2. Start the RAG server
uvicorn app.main:app --reload

# 3. Ingest documents
curl -X POST http://localhost:8000/ingest

# 4. Ask questions (uses Ollama llama3:8b)
curl -X POST http://localhost:8000/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the eligibility criteria?"}'
```

## Advantages of Ollama

✅ **Free**: No API costs  
✅ **Private**: Data never leaves your machine  
✅ **Fast**: Local inference, no network latency  
✅ **Offline**: Works without internet  
✅ **No Rate Limits**: Use as much as you want  

## System Requirements

### Minimum
- **RAM**: 8GB
- **Storage**: 5GB free space
- **CPU**: Modern multi-core processor

### Recommended
- **RAM**: 16GB+
- **Storage**: 10GB+ free space
- **GPU**: NVIDIA GPU with CUDA support (optional, for faster inference)

## Troubleshooting

### Ollama not responding
```bash
# Check if Ollama is running
ps aux | grep ollama

# Restart Ollama
killall ollama
ollama serve
```

### Model not found
```bash
# Re-pull the model
ollama pull llama3:8b

# List available models
ollama list
```

### Slow inference
- Ensure you have enough RAM (8GB minimum)
- Close other applications
- Consider using a smaller model: `ollama pull llama3:3b`

### Connection refused
```bash
# Verify Ollama is listening on port 11434
lsof -i :11434

# Check Ollama logs
journalctl -u ollama -f  # Linux
# or check system logs on macOS/Windows
```

## Alternative Models

You can use other models by changing the model name in `llm.py`:

```python
# Smaller, faster model
ChatOllama(model="llama3:3b", temperature=0)

# Larger, more capable model
ChatOllama(model="llama3:70b", temperature=0)

# Other models
ChatOllama(model="mistral", temperature=0)
ChatOllama(model="codellama", temperature=0)
```

See all available models: https://ollama.ai/library

## Performance Tips

1. **Use GPU**: Ollama automatically uses GPU if available
2. **Increase context window**: Add `num_ctx=4096` parameter
3. **Adjust temperature**: Keep at 0 for factual answers
4. **Monitor resources**: Use `htop` or Activity Monitor

## Support

- Ollama Documentation: https://github.com/ollama/ollama
- Ollama Discord: https://discord.gg/ollama
- LangChain Ollama: https://python.langchain.com/docs/integrations/chat/ollama

---

**Ready to use!** Once Ollama is installed and llama3:8b is pulled, the RAG system will work seamlessly. 🚀
