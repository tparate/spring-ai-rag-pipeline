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
 * Recursive character splitter, in the style popularized by LangChain: tries to split on the
 * "highest quality" separator first (paragraph breaks), and only falls back to a lower-quality
 * separator (line, then sentence, then raw characters) for pieces that are still too large after
 * the previous pass. This generally gives the best balance of structural coherence and
 * predictable chunk-size bounds - a good default for mixed/unpredictable document quality.
 */
@Component
public class RecursiveCharacterChunkingStrategy implements ChunkingStrategy {

    private static final String[] SEPARATORS = {"\n\n", "\n", ". ", " "};

    @Override
    public ChunkingStrategyType getType() {
        return ChunkingStrategyType.RECURSIVE_CHARACTER;
    }

    @Override
    public List<Document> chunk(List<Document> documents, ChunkingOptions options) {
        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {
            String text = doc.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            List<String> pieces = split(text, 0, options.chunkSize());
            List<String> merged = mergeWithOverlap(pieces, options);

            int chunkIndex = 0;
            for (String piece : merged) {
                if (piece.trim().length() >= options.minChunkSizeChars()) {
                    result.add(newChunk(doc, piece.trim(), chunkIndex++));
                }
            }
        }
        return result;
    }

    /** Recursively splits text on the best available separator until every piece fits chunkSize. */
    private List<String> split(String text, int separatorIndex, int chunkSize) {
        if (text.length() <= chunkSize || separatorIndex >= SEPARATORS.length) {
            return List.of(text);
        }
        String separator = SEPARATORS[separatorIndex];
        String[] parts = text.split(java.util.regex.Pattern.quote(separator));

        List<String> pieces = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (part.length() > chunkSize) {
                // still too big at this separator level -> recurse to the next, finer-grained one
                pieces.addAll(split(part, separatorIndex + 1, chunkSize));
            } else {
                pieces.add(part);
            }
        }
        return pieces;
    }

    /** Re-groups small pieces up to chunkSize and carries a small overlap into the next chunk. */
    private List<String> mergeWithOverlap(List<String> pieces, ChunkingOptions options) {
        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String piece : pieces) {
            if (current.length() + piece.length() + 1 > options.chunkSize() && !current.isEmpty()) {
                String chunk = current.toString();
                merged.add(chunk);
                // seed the next chunk with a trailing slice of this one, for context continuity
                int overlapStart = Math.max(0, chunk.length() - options.chunkOverlap());
                current = new StringBuilder(chunk.substring(overlapStart));
            }
            current.append(piece).append(' ');
        }
        if (!current.isEmpty()) {
            merged.add(current.toString());
        }
        return merged;
    }

    private Document newChunk(Document source, String text, int chunkIndex) {
        var metadata = new HashMap<>(source.getMetadata());
        metadata.put("chunk_index", chunkIndex);
        metadata.put("chunk_strategy", ChunkingStrategyType.RECURSIVE_CHARACTER.name());
        return new Document(text, metadata);
    }
}
