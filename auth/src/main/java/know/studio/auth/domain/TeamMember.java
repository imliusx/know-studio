package know.studio.auth.domain;

import know.studio.auth.api.TeamRole;

public record TeamMember(UserAccount user, TeamRole role) {
}
