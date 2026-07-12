package know.studio.arag.knowledge.api;

import know.studio.arag.platform.core.json.JsonLongId;

public record KnowledgeBaseTeamGrantInfo(@JsonLongId long teamId, KnowledgeBasePermission permission) {
}
