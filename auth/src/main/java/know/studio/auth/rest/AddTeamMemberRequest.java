package know.studio.auth.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import know.studio.auth.api.TeamRole;

public record AddTeamMemberRequest(
        @NotBlank @Email String email,
        @NotNull TeamRole role
) {
}
