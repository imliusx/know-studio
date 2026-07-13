package know.studio.search.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import know.studio.search.api.RetrievalMode;

import java.util.Set;

public record RetrievalRequest(
        @NotBlank String question,
        Set<@Positive Long> knowledgeBaseIds,
        @Min(1) @Max(20) Integer topK,
        RetrievalMode mode
) {
}
