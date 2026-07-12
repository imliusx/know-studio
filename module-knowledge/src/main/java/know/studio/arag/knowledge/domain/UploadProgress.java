package know.studio.arag.knowledge.domain;

import know.studio.arag.platform.core.json.JsonLongId;

import java.util.List;

public record UploadProgress(
        @JsonLongId long uploadSessionId,
        int totalChunks,
        List<Integer> uploadedChunks,
        UploadStatus status,
        @JsonLongId Long documentId
) {

    public UploadProgress {
        uploadedChunks = List.copyOf(uploadedChunks);
    }
}
