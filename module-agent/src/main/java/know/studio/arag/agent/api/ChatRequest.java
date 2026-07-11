package know.studio.arag.agent.api;

public record ChatRequest(
        long sessionId,
        long workspaceId,
        String message,
        boolean toolMode,
        boolean deepThinking
) {

    public ChatRequest {
        if (sessionId <= 0 || workspaceId <= 0) {
            throw new IllegalArgumentException("sessionId and workspaceId must be positive");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        message = message.trim();
    }
}
