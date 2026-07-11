package know.studio.arag.identity.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import know.studio.arag.identity.api.WorkspaceRole;

public record AddWorkspaceMemberRequest(
        @NotBlank @Email String email,
        @NotNull WorkspaceRole role
) {
}
