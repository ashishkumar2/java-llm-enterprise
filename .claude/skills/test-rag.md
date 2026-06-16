# Skill: test-rag

Smoke-test the full RAG pipeline end-to-end from the command line.

## Usage
`/test-rag` — runs through ingest → wait → chat in sequence

## Prerequisites
All three services running and infrastructure healthy (see `/run-services`).

## Steps

### 1. Ingest a test document
```bash
# Create a small test doc
echo "The Enterprise LLM Platform uses pgvector for semantic search and OpenAI gpt-4 for generation." > /tmp/test-doc.txt

curl -s -X POST http://localhost:8080/api/ingest \
  -H "Authorization: Bearer $TEST_JWT" \
  -F "file=@/tmp/test-doc.txt" | jq .
```
Note the `documentId` from the response. Status should be `PROCESSING`.

### 2. Wait for ingestion to complete (~5-10 seconds)
```bash
# Poll until status is COMPLETED
DOCUMENT_ID="<paste id here>"
curl -s http://localhost:8082/api/ingest/$DOCUMENT_ID/status | jq .status
```

### 3. Query via chat
```bash
curl -s -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TEST_JWT" \
  -d '{
    "message": "What database does the platform use for semantic search?",
    "sessionId": "rag-smoke-test-1"
  }' | jq '{message: .message, tokens: .tokenCount}'
```

Expected: response mentioning `pgvector`.

### 4. Verify conversation memory
```bash
# Send a follow-up — the service should remember the previous turn
curl -s -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TEST_JWT" \
  -d '{
    "message": "What LLM model does it use for generation?",
    "sessionId": "rag-smoke-test-1"
  }' | jq .message
```

Expected: response mentioning `gpt-4` (from the document) and context from the prior turn.
