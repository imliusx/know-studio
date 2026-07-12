package know.studio.arag.knowledge.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import know.studio.arag.knowledge.api.KnowledgeBaseVisibility;

public record UpdateKnowledgeBaseRequest(
        @NotBlank String name,
        String description,
        @NotNull KnowledgeBaseVisibility visibility,
        @Positive Long ownerTeamId
) {
}
