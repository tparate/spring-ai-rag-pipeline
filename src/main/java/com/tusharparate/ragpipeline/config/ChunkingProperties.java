package com.tusharparate.ragpipeline.config;

import com.tusharparate.ragpipeline.chunking.ChunkingStrategyType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the app.chunking.* block in application.yml - used whenever a request doesn't override values. */
@ConfigurationProperties(prefix = "app.chunking")
public record ChunkingProperties(
        ChunkingStrategyType defaultStrategy,
        int chunkSize,
        int chunkOverlap,
        int minChunkSizeChars
) {
}
