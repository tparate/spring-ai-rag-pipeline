package com.tusharparate.ragpipeline.service;

import com.tusharparate.ragpipeline.chunking.ChunkingOptions;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategyType;
import com.tusharparate.ragpipeline.dto.IngestResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentIngestionService {

    /**
     * Reads, chunks, embeds, and stores the given file in the vector store.
     *
     * @param file     the uploaded document (pdf, docx, txt, html, pptx - anything Tika supports)
     * @param strategy which chunking method to use for this file
     * @param options  chunk size/overlap/min-size parameters
     */
    IngestResponse ingest(MultipartFile file, ChunkingStrategyType strategy, ChunkingOptions options);
}
