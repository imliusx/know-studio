package know.studio.arag.evaluation.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AddSampleRequest(
        @NotBlank @Size(max = 2_000) String question,
        List<Long> relevantChunkIds,
        @Size(max = 10_000) String expectedAnswer,
        boolean expectRefusal
) {
}
