package know.studio.arag.conversation.api;

public record CreateSessionCommand(
        long workspaceId,
        String title,
        boolean toolMode,
        boolean deepThinking
) {
}
