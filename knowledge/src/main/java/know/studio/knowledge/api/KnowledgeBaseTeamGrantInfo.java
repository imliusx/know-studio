package know.studio.knowledge.api;

import know.studio.common.json.JsonLongId;

public record KnowledgeBaseTeamGrantInfo(@JsonLongId long teamId, KnowledgeBasePermission permission) {
}
