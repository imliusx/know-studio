package know.studio.knowledge.domain;

import know.studio.common.json.JsonLongId;

import java.util.List;

public record UploadInitResult(
        boolean instantUpload,
        @JsonLongId Long documentId,
        @JsonLongId Long uploadSessionId,
        List<Integer> uploadedChunks
) {

    public UploadInitResult {
        uploadedChunks = uploadedChunks == null ? List.of() : List.copyOf(uploadedChunks);
    }
}
