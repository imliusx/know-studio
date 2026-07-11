package know.studio.arag.knowledge.domain;

public record UploadChunk(
        long id,
        long uploadSessionId,
        int chunkIndex,
        String objectKey,
        long chunkSize,
        String chunkHash
) {
}
