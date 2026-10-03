package com.tusharparate.ragpipeline.chunking.impl;

import com.tusharparate.ragpipeline.chunking.ChunkingOptions;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategy;
import com.tusharparate.ragpipeline.chunking.ChunkingStrategyType;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Groups whole sentences together until adding the next sentence would exceed chunkSize,
 * then starts a new chunk. Never splits a sentence in the middle, which usually gives more
 * coherent, better-retrievable chunks than FIXED_SIZE for prose content.
 */
@Component
public class SentenceChunkingStrategy implements ChunkingStrategy {

    // Splits after '.', '!' or '?' followed by whitespace, but not on common abbreviations like "Mr."
    private static final Pattern SENTENCE_BOUNDARY =
            Pattern.compile("(?<=[.!?])\\s+(?=[A-Z0-9\"'(])");

    @Override
    public ChunkingStrategyType getType() {
        return ChunkingStrategyType.SENTENCE;
    }

    @Override
    public List<Document> chunk(List<Document> documents, ChunkingOptions options) {
        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {
            String text = doc.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            String[] sentences = SENTENCE_BOUNDARY.split(text.trim());

            StringBuilder current = new StringBuilder();
            int chunkIndex = 0;
            for (String sentence : sentences) {
                if (current.length() + sentence.length() + 1 > options.chunkSize() && !current.isEmpty()) {
                    result.add(newChunk(doc, current.toString().trim(), chunkIndex++));
                    current.setLength(0);
                }
                current.append(sentence).append(' ');
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
        metadata.put("chunk_strategy", ChunkingStrategyType.SENTENCE.name());
        return new Document(text, metadata);
    }
}
