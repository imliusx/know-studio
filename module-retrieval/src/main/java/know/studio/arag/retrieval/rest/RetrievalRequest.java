package know.studio.arag.retrieval.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import know.studio.arag.retrieval.api.RetrievalMode;

public record RetrievalRequest(
        @NotBlank String question,
        @Min(1) @Max(20) Integer topK,
        RetrievalMode mode
) {
}
