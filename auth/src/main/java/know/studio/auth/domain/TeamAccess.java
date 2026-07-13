package know.studio.auth.domain;

import know.studio.auth.api.TeamRole;

public record TeamAccess(Team team, TeamRole role) {
}
