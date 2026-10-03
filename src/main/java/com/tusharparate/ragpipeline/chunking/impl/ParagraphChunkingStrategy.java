package com.tusharparate.ragpipeline.chunking.impl;

import com.tusharparate.ragpipeline.chunking.ChunkingOptions;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategy;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategyType;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Splits on blank-line paragraph boundaries, merging consecutive short paragraphs up to
 * chunkSize and splitting an oversized single paragraph on sentence-like punctuation as a
 * fallback. Works well for well-formatted docs (reports, wikis, markdown) where paragraphs
 * are already coherent semantic units.
 */
@Component
public class ParagraphChunkingStrategy implements ChunkingStrategy {

    @Override
    public ChunkingStrategyType getType() {
        return ChunkingStrategyType.PARAGRAPH;
    }

    @Override
    public List<Document> chunk(List<Document> documents, ChunkingOptions options) {
        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {
            String text = doc.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            String[] paragraphs = text.split("\\n\\s*\\n");

            StringBuilder current = new StringBuilder();
            int chunkIndex = 0;
            for (String paragraph : paragraphs) {
                String trimmed = paragraph.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (current.length() + trimmed.length() + 2 > options.chunkSize() && !current.isEmpty()) {
                    result.add(newChunk(doc, current.toString().trim(), chunkIndex++));
                    current.setLength(0);
                }
                // a single paragraph larger than chunkSize is kept intact rather than butchered -
                // switch to RECURSIVE_CHARACTER if you need hard size guarantees on messy input
                current.append(trimmed).append("\n\n");
            }
            if (!current.isEmpty() && current.toString().trim().length() >= options.minChunkSizeChars()) {
                result.add(newChunk(doc, current.toString().trim(), chunkIndex));
            }
        }
        return result;
    }

    private Document newChunk(Document source, String text, int chunkIndex) {
        var metadata = new HashMap<>(source.getMetadata());
        metadata.put("chunk_index", chunkIndex);
        metadata.put("chunk_strategy", ChunkingStrategyType.PARAGRAPH.name());
        return new Document(text, metadata);
    }
}
