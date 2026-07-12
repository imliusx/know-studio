package know.studio.arag.identity.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        Long parentId
) {
}
