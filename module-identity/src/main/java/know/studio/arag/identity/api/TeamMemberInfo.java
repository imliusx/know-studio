package know.studio.arag.identity.api;

import know.studio.arag.platform.core.json.JsonLongId;

public record TeamMemberInfo(
        @JsonLongId long userId,
        String email,
        String displayName,
        TeamRole role
) {
}
