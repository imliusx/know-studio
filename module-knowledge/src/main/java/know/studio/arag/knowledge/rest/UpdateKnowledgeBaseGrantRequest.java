package know.studio.arag.knowledge.rest;

import jakarta.validation.constraints.NotNull;
import know.studio.arag.knowledge.api.KnowledgeBasePermission;

public record UpdateKnowledgeBaseGrantRequest(@NotNull KnowledgeBasePermission permission) {
}
