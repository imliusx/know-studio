package know.studio.arag.retrieval.api;

import java.util.Set;

public record RetrievalQuery(String question, Set<Long> knowledgeBaseIds, int topK, RetrievalMode mode) {

    public RetrievalQuery(String question, Set<Long> knowledgeBaseIds, int topK) {
        this(question, knowledgeBaseIds, topK, RetrievalMode.HYBRID_RERANK);
    }

    public RetrievalQuery {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        question = question.trim();
        knowledgeBaseIds = knowledgeBaseIds == null ? Set.of() : Set.copyOf(knowledgeBaseIds);
        if (knowledgeBaseIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("knowledgeBaseIds must contain only positive IDs");
        }
        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException("topK must be between 1 and 20");
        }
        mode = mode == null ? RetrievalMode.HYBRID_RERANK : mode;
    }
}
