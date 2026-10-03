package com.tusharparate.ragpipeline.chunking.impl;

import com.tusharparate.ragpipeline.chunking.ChunkingOptions;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategy;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategyType;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Splits by estimated token count rather than raw characters - generally the best default
 * for RAG, since it keeps chunks aligned with what the embedding model actually "sees" as
 * a unit, instead of accidentally cutting a chunk mid-token.
 */
@Component
public class TokenChunkingStrategy implements ChunkingStrategy {

    @Override
    public ChunkingStrategyType getType() {
        return ChunkingStrategyType.TOKEN;
    }

    @Override
    public List<Document> chunk(List<Document> documents, ChunkingOptions options) {
        // TokenTextSplitter(chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator)
        TokenTextSplitter splitter = new TokenTextSplitter(
                options.chunkSize(),
                options.minChunkSizeChars(),
                5,          // minChunkLengthToEmbed - drop near-empty trailing fragments
                10_000,     // maxNumChunks - generous ceiling, adjust if you ingest huge docs
                true        // keepSeparator - preserves newlines for readability
        );
        return splitter.apply(documents);
    }
}
