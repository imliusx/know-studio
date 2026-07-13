package know.studio.knowledge.domain;

public record UploadChunk(
        long id,
        long uploadSessionId,
        int chunkIndex,
        String objectKey,
        long chunkSize,
        String chunkHash
) {
}
