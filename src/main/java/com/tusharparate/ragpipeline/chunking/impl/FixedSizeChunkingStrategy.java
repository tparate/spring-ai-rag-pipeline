package com.tusharparate.ragpipeline.chunking.impl;

import com.tusharparate.ragpipeline.chunking.ChunkingOptions;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategy;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategyType;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The simplest possible strategy: slices raw text into fixed-length windows with overlap,
 * ignoring sentence/paragraph structure entirely. Useful as a fast baseline or for content
 * that has no real structure (logs, transcripts without punctuation, etc.), but generally
 * the weakest choice for prose - prefer TOKEN or RECURSIVE_CHARACTER for real documents.
 */
@Component
public class FixedSizeChunkingStrategy implements ChunkingStrategy {

    @Override
    public ChunkingStrategyType getType() {
        return ChunkingStrategyType.FIXED_SIZE;
    }

    @Override
    public List<Document> chunk(List<Document> documents, ChunkingOptions options) {
        List<Document> result = new ArrayList<>();
        int size = options.chunkSize();
        int overlap = Math.min(options.chunkOverlap(), size - 1);
        int step = size - overlap;

        for (Document doc : documents) {
            String text = doc.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            int pos = 0;
            int chunkIndex = 0;
            while (pos < text.length()) {
                int end = Math.min(pos + size, text.length());
                String chunkText = text.substring(pos, end);
                if (chunkText.trim().length() >= options.minChunkSizeChars() || end == text.length()) {
                    result.add(newChunk(doc, chunkText, chunkIndex++));
                }
                if (end == text.length()) {
                    break;
                }
                pos += step;
            }
        }
        return result;
    }

    private Document newChunk(Document source, String text, int chunkIndex) {
        var metadata = new java.util.HashMap<>(source.getMetadata());
        metadata.put("chunk_index", chunkIndex);
        metadata.put("chunk_strategy", ChunkingStrategyType.FIXED_SIZE.name());
        return new Document(text, metadata);
    }
}
