package know.studio.arag.knowledge.domain;

import java.util.List;

public record UploadProgress(
        long uploadSessionId,
        int totalChunks,
        List<Integer> uploadedChunks,
        UploadStatus status,
        Long documentId
) {

    public UploadProgress {
        uploadedChunks = List.copyOf(uploadedChunks);
    }
}
