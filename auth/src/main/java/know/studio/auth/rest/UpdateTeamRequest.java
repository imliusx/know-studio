package know.studio.auth.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record UpdateTeamRequest(
        @NotBlank String name,
        String description,
        @Positive Long parentId
) {
}
