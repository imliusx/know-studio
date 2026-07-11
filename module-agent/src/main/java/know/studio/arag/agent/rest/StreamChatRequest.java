package know.studio.arag.agent.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record StreamChatRequest(
        @Positive long sessionId,
        @NotBlank String message,
        boolean toolMode,
        boolean deepThinking
) {
}
