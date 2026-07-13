package know.studio.knowledge.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InitiateUploadRequest(
        @NotBlank @Size(max = 255) String fileName,
        @Size(max = 150) String contentType,
        @Min(1) long fileSize,
        @NotBlank @Pattern(regexp = "(?i)[a-f0-9]{64}") String contentHash,
        @Min(1) @Max(10_000) int totalChunks
) {
}
