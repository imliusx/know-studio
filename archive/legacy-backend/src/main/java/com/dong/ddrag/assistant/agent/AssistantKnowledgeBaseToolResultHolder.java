package com.dong.ddrag.assistant.agent;

import com.dong.ddrag.assistant.model.vo.tool.KnowledgeBaseSearchToolResponse;
import com.dong.ddrag.qa.model.vo.AskQuestionResponse;

import java.util.List;
import java.util.Optional;

public class AssistantKnowledgeBaseToolResultHolder {

    private List<AskQuestionResponse.Citation> citations = List.of();
    private KnowledgeBaseSearchToolResponse searchResponse;
    private KnowledgeBaseSearchState searchState = KnowledgeBaseSearchState.NOT_STARTED;

    public void recordSearchResponse(KnowledgeBaseSearchToolResponse searchResponse) {
        this.searchResponse = searchResponse;
        List<AskQuestionResponse.Citation> citations = searchResponse == null ? List.of() : searchResponse.citations();
        this.citations = citations == null ? List.of() : List.copyOf(citations);
        this.searchState = KnowledgeBaseSearchState.COMPLETED;
    }

    public List<AskQuestionResponse.Citation> currentCitations() {
        return citations;
    }

    public Optional<KnowledgeBaseSearchToolResponse> currentSearchResponse() {
        return Optional.ofNullable(searchResponse);
    }

    public boolean hasCompletedSearch() {
        return searchState == KnowledgeBaseSearchState.COMPLETED;
    }

    enum KnowledgeBaseSearchState {
        NOT_STARTED,
        COMPLETED
    }
}
