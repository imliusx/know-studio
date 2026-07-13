package know.studio.eval.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDatasetRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2_000) String description
) {
}
