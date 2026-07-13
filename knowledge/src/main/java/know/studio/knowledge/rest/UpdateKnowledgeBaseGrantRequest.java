package know.studio.knowledge.rest;

import jakarta.validation.constraints.NotNull;
import know.studio.knowledge.api.KnowledgeBasePermission;

public record UpdateKnowledgeBaseGrantRequest(@NotNull KnowledgeBasePermission permission) {
}
