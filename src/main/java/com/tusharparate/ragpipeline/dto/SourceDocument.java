package com.tusharparate.ragpipeline.dto;

import java.util.Map;

public record SourceDocument(
        String id,
        String contentSnippet,
        Map<String, Object> metadata
) {
}
