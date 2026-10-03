package com.tusharparate.ragpipeline.controller;

import com.tusharparate.ragpipeline.dto.ChatRequest;
import com.tusharparate.ragpipeline.dto.ChatResponse;
import com.tusharparate.ragpipeline.service.RagQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final RagQueryService ragQueryService;

    /**
     * Example:
     * curl -X POST http://localhost:8080/api/chat/query \
     *   -H "Content-Type: application/json" \
     *   -d '{"question": "What is the refund policy?", "topK": 5, "similarityThreshold": 0.6}'
     */
    @PostMapping("/api/chat/query")
    public ResponseEntity<ChatResponse> query(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(ragQueryService.query(request));
    }
}
