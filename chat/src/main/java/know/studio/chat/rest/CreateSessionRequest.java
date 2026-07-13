package know.studio.chat.rest;

import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @Size(max = 200) String title,
        boolean toolMode,
        boolean deepThinking
) {
}
