package know.studio.arag.knowledge.domain;

import know.studio.arag.platform.core.json.JsonLongId;

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
