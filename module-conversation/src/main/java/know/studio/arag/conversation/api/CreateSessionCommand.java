package know.studio.arag.conversation.api;

public record CreateSessionCommand(
        String title,
        boolean toolMode,
        boolean deepThinking
) {
}
