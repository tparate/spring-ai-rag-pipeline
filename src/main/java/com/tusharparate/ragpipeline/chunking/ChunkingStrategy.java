package com.tusharparate.ragpipeline.chunking;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Implemented by each concrete chunking method. Every implementation is a Spring bean
 * discovered automatically by {@link ChunkingStrategyFactory} - to add a new chunking
 * method, add a new enum value in {@link ChunkingStrategyType} and one new class here,
 * nothing else needs to change.
 */
public interface ChunkingStrategy {

    /** Which strategy type this implementation handles. */
    ChunkingStrategyType getType();

    /** Splits the given source documents into smaller chunk documents ready for embedding. */
    List<Document> chunk(List<Document> documents, ChunkingOptions options);
}
