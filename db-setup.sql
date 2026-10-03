-- Run once against your existing localhost:5432 Postgres instance.
-- (Skip manually creating vector_store table/extensions - Spring AI does that
--  automatically on startup since application.yml sets initialize-schema: true.
--  This script is only needed if you'd rather control schema creation yourself.)

CREATE DATABASE demo_rag_db ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C' TEMPLATE template0;

\c demo_rag_db

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
