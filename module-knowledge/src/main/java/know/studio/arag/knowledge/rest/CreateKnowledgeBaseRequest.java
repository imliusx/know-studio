package know.studio.arag.knowledge.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import know.studio.arag.knowledge.api.KnowledgeBaseVisibility;

public record CreateKnowledgeBaseRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull KnowledgeBaseVisibility visibility,
        Long ownerTeamId
) {
}
