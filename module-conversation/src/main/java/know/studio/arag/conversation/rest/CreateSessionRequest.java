package know.studio.arag.conversation.rest;

import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @Size(max = 200) String title,
        boolean toolMode,
        boolean deepThinking
) {
}
