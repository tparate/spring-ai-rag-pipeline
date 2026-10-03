package com.tusharparate.ragpipeline.controller;

import com.tusharparate.ragpipeline.chunking.ChunkingOptions;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategyType;
import com.tusharparate.ragpipeline.config.ChunkingProperties;
import com.tusharparate.ragpipeline.dto.IngestResponse;
import com.tusharparate.ragpipeline.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;
    private final ChunkingProperties chunkingProperties;

    /**
     * Uploads and ingests a document, chunking it with the requested strategy.
     * Any parameter left out falls back to the app.chunking.* defaults in application.yml.
     *
     * Example:
     * curl -X POST http://localhost:8080/api/documents/ingest \
     *   -F "file=@handbook.pdf" \
     *   -F "strategy=RECURSIVE_CHARACTER" \
     *   -F "chunkSize=1000" \
     *   -F "chunkOverlap=150"
     */
    @PostMapping(value = "/api/documents/ingest", consumes = "multipart/form-data")
    public ResponseEntity<IngestResponse> ingest(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) ChunkingStrategyType strategy,
            @RequestParam(required = false) Integer chunkSize,
            @RequestParam(required = false) Integer chunkOverlap,
            @RequestParam(required = false) Integer minChunkSizeChars
    ) {
        ChunkingStrategyType resolvedStrategy =
                strategy != null ? strategy : chunkingProperties.defaultStrategy();

        ChunkingOptions options = new ChunkingOptions(
                chunkSize != null ? chunkSize : chunkingProperties.chunkSize(),
                chunkOverlap != null ? chunkOverlap : chunkingProperties.chunkOverlap(),
                minChunkSizeChars != null ? minChunkSizeChars : chunkingProperties.minChunkSizeChars()
        );

        IngestResponse response = documentIngestionService.ingest(file, resolvedStrategy, options);
        return ResponseEntity.ok(response);
    }
}
