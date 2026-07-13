package know.studio.auth.rest;

import jakarta.validation.constraints.NotNull;
import know.studio.auth.api.TeamRole;

public record UpdateTeamMemberRequest(@NotNull TeamRole role) {
}
