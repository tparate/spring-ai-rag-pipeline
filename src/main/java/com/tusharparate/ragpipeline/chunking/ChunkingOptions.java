package com.tusharparate.ragpipeline.chunking;

/**
 * Parameters shared across chunking strategies. Not every strategy uses every field
 * (e.g. SENTENCE ignores overlap), but keeping one shape makes the factory/controller simple.
 *
 * @param chunkSize          target size per chunk (characters for FIXED_SIZE/SENTENCE/PARAGRAPH/
 *                            RECURSIVE_CHARACTER, estimated tokens for TOKEN)
 * @param chunkOverlap       how much adjacent chunks should overlap (same unit as chunkSize)
 * @param minChunkSizeChars  chunks smaller than this get merged into a neighbor instead of
 *                            being embedded as their own near-empty vector
 */
public record ChunkingOptions(
        int chunkSize,
        int chunkOverlap,
        int minChunkSizeChars
) {
    public static ChunkingOptions defaults() {
        return new ChunkingOptions(800, 100, 350);
    }
}
