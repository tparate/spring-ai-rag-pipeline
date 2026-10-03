package com.tusharparate.ragpipeline.dto;

import com.tusharparate.ragpipeline.chunking.ChunkingStrategyType;

public record IngestResponse(
        String fileName,
        ChunkingStrategyType strategyUsed,
        int chunkCount
) {
}
