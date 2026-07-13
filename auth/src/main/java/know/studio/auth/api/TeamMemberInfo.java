package know.studio.auth.api;

import know.studio.common.json.JsonLongId;

public record TeamMemberInfo(
        @JsonLongId long userId,
        String email,
        String displayName,
        TeamRole role
) {
}
