package know.studio.arag.identity.api;

import know.studio.arag.platform.core.json.JsonLongId;

public record TeamInfo(
        @JsonLongId long teamId,
        String name,
        String description,
        TeamRole role
) {
}
