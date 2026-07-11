package know.studio.arag.knowledge.domain;

import java.util.List;

public record UploadInitResult(
        boolean instantUpload,
        Long documentId,
        Long uploadSessionId,
        List<Integer> uploadedChunks
) {

    public UploadInitResult {
        uploadedChunks = uploadedChunks == null ? List.of() : List.copyOf(uploadedChunks);
    }
}
