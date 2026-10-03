package com.tusharparate.ragpipeline.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "question must not be blank")
        String question,

        // optional overrides - null means "use the service defaults"
        Integer topK,
        Double similarityThreshold
) {
}
