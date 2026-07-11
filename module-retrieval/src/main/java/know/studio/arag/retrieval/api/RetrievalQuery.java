package know.studio.arag.retrieval.api;

public record RetrievalQuery(String question, long workspaceId, int topK, RetrievalMode mode) {

    public RetrievalQuery(String question, long workspaceId, int topK) {
        this(question, workspaceId, topK, RetrievalMode.HYBRID_RERANK);
    }

    public RetrievalQuery {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        question = question.trim();
        if (workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId must be positive");
        }
        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException("topK must be between 1 and 20");
        }
        mode = mode == null ? RetrievalMode.HYBRID_RERANK : mode;
    }
}
