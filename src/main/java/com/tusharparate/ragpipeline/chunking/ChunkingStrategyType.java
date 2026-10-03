package com.tusharparate.ragpipeline.chunking;

/**
 * The chunking methods available for document ingestion.
 * Pass one of these (as a query param or request field) to choose how a document is split.
 */
public enum ChunkingStrategyType {

    /** Splits purely by character count, with a fixed overlap. Fast, naive, ignores structure. */
    FIXED_SIZE,

    /** Splits by estimated token count using Spring AI's TokenTextSplitter (jtokkit under the hood). */
    TOKEN,

    /** Splits on sentence boundaries, grouping sentences until the target chunk size is reached. */
    SENTENCE,

    /** Splits on blank-line paragraph boundaries. Best for well-formatted docs (reports, articles). */
    PARAGRAPH,

    /** LangChain-style recursive splitter: tries paragraph, then line, then sentence, then word
     *  boundaries in order, only falling back to a harder split when a chunk is still too large. */
    RECURSIVE_CHARACTER
}
