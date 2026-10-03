package com.tusharparate.ragpipeline.service.impl;

import com.tusharparate.ragpipeline.dto.ChatRequest;
import com.tusharparate.ragpipeline.dto.ChatResponse;
import com.tusharparate.ragpipeline.dto.SourceDocument;
import com.tusharparate.ragpipeline.service.RagQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RagQueryServiceImpl implements RagQueryService {

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.5;
    private static final int SNIPPET_LENGTH = 200;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagQueryServiceImpl(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    @Override
    public ChatResponse query(ChatRequest request) {
        int topK = request.topK() != null ? request.topK() : DEFAULT_TOP_K;
        double threshold = request.similarityThreshold() != null
                ? request.similarityThreshold() : DEFAULT_SIMILARITY_THRESHOLD;

        SearchRequest searchRequest = SearchRequest.builder()
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        // Run the retrieval once ourselves too, purely so we can return the sources to the caller -
        // the advisor performs its own equivalent search internally when augmenting the prompt.
        List<Document> retrieved = vectorStore.similaritySearch(
                SearchRequest.builder().query(request.question()).topK(topK).similarityThreshold(threshold).build());

        String answer = chatClient.prompt()
                .advisors(advisor)
                .user(request.question())
                .call()
                .content();

        List<SourceDocument> sources = retrieved.stream()
                .map(this::toSourceDocument)
                .toList();

        return new ChatResponse(answer, sources);
    }

    private SourceDocument toSourceDocument(Document document) {
        String text = document.getText() == null ? "" : document.getText();
        String snippet = text.length() > SNIPPET_LENGTH ? text.substring(0, SNIPPET_LENGTH) + "..." : text;
        return new SourceDocument(document.getId(), snippet, document.getMetadata());
    }
}
