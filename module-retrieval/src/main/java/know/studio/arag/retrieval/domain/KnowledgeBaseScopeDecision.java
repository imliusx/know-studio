package know.studio.arag.retrieval.domain;

import java.util.Set;

public record KnowledgeBaseScopeDecision(Set<Long> knowledgeBaseIds, double confidence) {

    public KnowledgeBaseScopeDecision {
        knowledgeBaseIds = knowledgeBaseIds == null ? Set.of() : Set.copyOf(knowledgeBaseIds);
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
    }

    public static KnowledgeBaseScopeDecision uncertain() {
        return new KnowledgeBaseScopeDecision(Set.of(), 0.0);
    }
}
