# Spring AI RAG Pipeline

A modular Retrieval-Augmented Generation pipeline built with Spring Boot + Spring AI,
backed by PostgreSQL/pgvector, with a pluggable chunking strategy you choose per request.

## Project structure

```
spring-ai-rag-pipeline/
├── pom.xml
├── db-setup.sql
├── src/main/resources/
│   └── application.properties         # datasource, pgvector, model, chunking defaults
└── src/main/java/com/tusharparate/ragpipeline/
    ├── RagPipelineApplication.java
    ├── chunking/
    │   ├── ChunkingStrategyType.java       # enum: FIXED_SIZE, TOKEN, SENTENCE, PARAGRAPH, RECURSIVE_CHARACTER
    │   ├── ChunkingOptions.java            # chunkSize / chunkOverlap / minChunkSizeChars
    │   ├── ChunkingStrategy.java           # interface every strategy implements
    │   ├── ChunkingStrategyFactory.java    # resolves enum -> bean at runtime
    │   └── impl/
    │       ├── FixedSizeChunkingStrategy.java
    │       ├── TokenChunkingStrategy.java          # wraps Spring AI's TokenTextSplitter
    │       ├── SentenceChunkingStrategy.java
    │       ├── ParagraphChunkingStrategy.java
    │       └── RecursiveCharacterChunkingStrategy.java  # LangChain-style recursive splitter
    ├── config/
    │   ├── ChunkingProperties.java     # binds app.chunking.* from application.yml
    │   └── WebConfig.java              # CORS for local frontend testing
    ├── controller/
    │   ├── DocumentController.java     # POST /api/documents/ingest
    │   └── ChatController.java         # POST /api/chat/query
    ├── service/
    │   ├── DocumentIngestionService.java
    │   ├── RagQueryService.java
    │   └── impl/
    │       ├── DocumentIngestionServiceImpl.java
    │       └── RagQueryServiceImpl.java
    ├── dto/
    │   ├── IngestResponse.java
    │   ├── ChatRequest.java
    │   ├── ChatResponse.java
    │   └── SourceDocument.java
    └── exception/
        ├── RagPipelineException.java
        └── GlobalExceptionHandler.java
```

Standard layering throughout: **Controller → Service interface → Service impl**, plus a
separate **Chunking strategy pattern** so new chunking methods drop in without touching
the ingestion service or controller at all.

## Setup steps

### 1. Database

If Postgres + pgvector are already running on `localhost:5432`, skip straight to creating the
database:
```bash
psql -U postgres -c "CREATE DATABASE demo_rag_db ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C' TEMPLATE template0;"
```
You do **not** need to manually `CREATE EXTENSION vector` — Spring AI does this automatically
at startup because `application.properties` sets `spring.ai.vectorstore.pgvector.initialize-schema=true`.
(`db-setup.sql` is included only if you'd rather run it yourself.)

#### Setting up Postgres 16 + pgvector from scratch (Windows)

Download the Postgres binaries from
[enterprisedb.com/download-postgresql-binaries](https://www.enterprisedb.com/download-postgresql-binaries)
(e.g. `postgresql-16.15-5-windows-x64-binaries.zip`).

1. **Extract Postgres 16.x** — unzip the binary archive to a permanent folder, e.g. `C:\pgsql16`.

2. **Add `<that path>\bin` to `PATH`** (System Properties → Environment Variables), open a new
   terminal, and verify:
   ```bash
   psql --version
   # Expected: psql (PostgreSQL) 16.x
   ```

3. **Initialize a data directory**:
   ```bash
   mkdir C:\pgsql16\data
   initdb -D "C:\pgsql16\data" -U postgres -E UTF8
   ```

4. **Start Postgres**:
   ```bash
   pg_ctl -D "C:\pgsql16\data" start
   pg_isready
   # Expected: localhost:5432 - accepting connections
   ```

5. **Install pgvector** — download the matching pgvector-for-pg16 build and copy the extracted
   files into your Postgres install's folders (adjust the path to wherever you extracted Postgres):
   ```bash
   copy vector.dll        C:\pgsql16\lib\
   copy vector.control    C:\pgsql16\share\extension\
   copy vector--*.sql     C:\pgsql16\share\extension\
   ```

6. **Create the database and enable the extension**:
   ```bash
   psql -U postgres -c "CREATE DATABASE demo_rag_db ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C' TEMPLATE template0;"
   psql -U postgres -d demo_rag_db -c "CREATE EXTENSION IF NOT EXISTS vector;"
   ```

7. **Point the app at it** — `localhost:5432`, database `demo_rag_db`, user `postgres` (default
   auth is trust, so no password), matching what's already in `application.properties`.

8. **Stop Postgres when done**:
   ```bash
   pg_ctl -D "C:\pgsql16\data" stop
   ```

### 2. Model (local Ollama — no API key needed)
This project runs entirely on a local Ollama install for both chat and embeddings. No
API key or internet connection required. Make sure Ollama is running and the models below
are pulled:
```bash
ollama pull llama3.2
ollama pull nomic-embed-text
ollama serve   # if it isn't already running as a background service
```
`application.properties` wires these up:
```properties
spring.ai.model.chat=ollama
spring.ai.model.embedding=ollama
spring.ai.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434}
spring.ai.ollama.chat.options.model=llama3.2
spring.ai.ollama.embedding.options.model=nomic-embed-text
```
**To switch to a hosted provider instead (e.g. OpenAI)**:
- Add the relevant starter (e.g. `spring-ai-starter-model-openai`) to `pom.xml`
- Set `spring.ai.model.chat`/`spring.ai.model.embedding` and the matching `spring.ai.<provider>.*`
  properties in `application.properties`
- Update `spring.ai.vectorstore.pgvector.dimensions` to match that model's embedding output size
  (this must be exact — a mismatch here is the most common pgvector setup error; `nomic-embed-text`
  is 768, OpenAI's `text-embedding-3-small` is 1536)

### 3. Build and run
```bash
mvn clean install
mvn spring-boot:run
```
On first startup, check the logs for `vector_store` table creation — confirms the schema
initialized correctly against `demo_rag_db`.

## Usage

### Ingest a document, choosing a chunking strategy
```bash
curl -X POST http://localhost:8091/api/documents/ingest \
  -F "file=@handbook.pdf" \
  -F "strategy=RECURSIVE_CHARACTER" \
  -F "chunkSize=1000" \
  -F "chunkOverlap=150"
```
`strategy` is optional — omit it to use `app.chunking.default-strategy` from `application.properties`
(defaults to `TOKEN`). Valid values: `FIXED_SIZE`, `TOKEN`, `SENTENCE`, `PARAGRAPH`, `RECURSIVE_CHARACTER`.

### Query
```bash
curl -X POST http://localhost:8091/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the refund policy?", "topK": 5, "similarityThreshold": 0.6}'
```
Response includes both the generated answer and the source chunks that were retrieved,
so you can verify grounding.

## Choosing a chunking strategy

| Strategy | Best for | Trade-off |
|---|---|---|
| `TOKEN` (default) | General-purpose, most content | Best alignment with embedding model's actual token boundaries |
| `RECURSIVE_CHARACTER` | Mixed-quality or unpredictable formatting | Best structural coherence + hard size guarantees |
| `PARAGRAPH` | Well-formatted docs (reports, markdown, wikis) | Can produce oversized chunks on a single huge paragraph |
| `SENTENCE` | Prose where mid-sentence cuts hurt retrieval quality | No hard overlap; chunk sizes vary more |
| `FIXED_SIZE` | Unstructured text (logs, transcripts) | Ignores structure entirely; weakest for prose |

## Adding a new chunking strategy later

1. Add a new value to `ChunkingStrategyType`.
2. Create a new `@Component` class in `chunking/impl/` implementing `ChunkingStrategy`.
3. Done — `ChunkingStrategyFactory` auto-discovers it via Spring's bean scanning; no other
   file needs to change.

## Notes / things to revisit before production

- `WebConfig` CORS origins are hardcoded to local dev ports — tighten before deploying.
- `RagQueryServiceImpl` runs similarity search twice (once via the advisor internally, once
  directly to build the `sources` list in the response) — acceptable for most workloads, but
  if latency matters, switch to Spring AI's `RetrievalAugmentationAdvisor` with a custom
  document-retriever callback to capture retrieved docs in one pass instead of two.
- No auth/rate-limiting on the endpoints — add before exposing beyond localhost.
- `spring.ai.vectorstore.pgvector.dimensions=768` in `application.properties` must match whichever
  embedding model you actually use (768 for `nomic-embed-text`; switching models requires updating
  this and recreating the `vector_store` table's embedding column, since it won't auto-migrate).

## License

MIT — see [LICENSE](LICENSE).
