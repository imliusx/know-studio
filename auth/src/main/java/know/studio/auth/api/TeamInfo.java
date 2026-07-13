package know.studio.auth.api;

import know.studio.common.json.JsonLongId;

public record TeamInfo(
        @JsonLongId long teamId,
        String name,
        String description,
        TeamRole role
) {
}
