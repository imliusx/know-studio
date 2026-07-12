package know.studio.arag.agent.api;

import java.util.Set;

public record ChatRequest(
        long sessionId,
        String message,
        Set<Long> knowledgeBaseIds,
        boolean toolMode,
        boolean deepThinking
) {

    public ChatRequest {
        if (sessionId <= 0) {
            throw new IllegalArgumentException("sessionId must be positive");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        message = message.trim();
        knowledgeBaseIds = knowledgeBaseIds == null ? Set.of() : Set.copyOf(knowledgeBaseIds);
        if (knowledgeBaseIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("knowledgeBaseIds must contain only positive IDs");
        }
    }
}
