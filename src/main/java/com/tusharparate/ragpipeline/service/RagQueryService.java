package com.tusharparate.ragpipeline.service;

import com.tusharparate.ragpipeline.dto.ChatRequest;
import com.tusharparate.ragpipeline.dto.ChatResponse;

public interface RagQueryService {

    /** Runs similarity search over the vector store and asks the chat model to answer using that context. */
    ChatResponse query(ChatRequest request);
}
