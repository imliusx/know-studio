package know.studio.arag.agent.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record StreamChatRequest(
        @Positive long sessionId,
        @NotBlank String message,
        Set<@Positive Long> knowledgeBaseIds,
        boolean toolMode,
        boolean deepThinking
) {
}
