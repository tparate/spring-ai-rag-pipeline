package com.tusharparate.ragpipeline.service.impl;

import com.tusharparate.ragpipeline.chunking.ChunkingOptions;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategy;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategyFactory;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategyType;
import com.tusharparate.ragpipeline.dto.IngestResponse;
import com.tusharparate.ragpipeline.exception.RagPipelineException;
import com.tusharparate.ragpipeline.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    private final ChunkingStrategyFactory chunkingStrategyFactory;
    private final VectorStore vectorStore;

    @Override
    public IngestResponse ingest(MultipartFile file, ChunkingStrategyType strategyType, ChunkingOptions options) {
        try {
            List<Document> rawDocuments = readDocument(file);
            tagSourceMetadata(rawDocuments, file);

            ChunkingStrategy strategy = chunkingStrategyFactory.resolve(strategyType);
            List<Document> chunks = strategy.chunk(rawDocuments, options);

            if (chunks.isEmpty()) {
                throw new RagPipelineException(
                        "Chunking produced zero chunks for '" + file.getOriginalFilename()
                                + "' - check that the file has extractable text and that chunkSize/minChunkSizeChars are sane.");
            }

            vectorStore.add(chunks);
            log.info("Ingested '{}' -> {} chunks using {} strategy",
                    file.getOriginalFilename(), chunks.size(), strategyType);

            return new IngestResponse(file.getOriginalFilename(), strategyType, chunks.size());

        } catch (IOException e) {
            throw new RagPipelineException("Failed to read uploaded file: " + file.getOriginalFilename(), e);
        }
    }

    private List<Document> readDocument(MultipartFile file) throws IOException {
        // TikaDocumentReader handles pdf/docx/pptx/txt/html/etc. uniformly via Apache Tika
        var resource = new InputStreamResource(file.getInputStream()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        return new TikaDocumentReader(resource).get();
    }

    private void tagSourceMetadata(List<Document> documents, MultipartFile file) {
        for (Document doc : documents) {
            Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
            metadata.put("source_file", file.getOriginalFilename());
            metadata.put("content_type", file.getContentType());
            doc.getMetadata().putAll(metadata);
        }
    }
}
