package know.studio.arag.identity.domain;

import know.studio.arag.identity.api.TeamRole;

public record TeamAccess(Team team, TeamRole role) {
}
