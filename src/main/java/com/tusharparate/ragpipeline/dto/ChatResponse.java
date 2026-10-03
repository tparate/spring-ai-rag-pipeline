package com.tusharparate.ragpipeline.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        List<SourceDocument> sources
) {
}
