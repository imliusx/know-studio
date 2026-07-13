package know.studio.chat.api;

public record CreateSessionCommand(
        String title,
        boolean toolMode,
        boolean deepThinking
) {
}
