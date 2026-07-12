package know.studio.arag.identity.rest;

import jakarta.validation.constraints.NotNull;
import know.studio.arag.identity.api.TeamRole;

public record UpdateTeamMemberRequest(@NotNull TeamRole role) {
}
