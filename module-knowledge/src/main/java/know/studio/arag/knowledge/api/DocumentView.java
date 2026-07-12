package know.studio.arag.knowledge.api;

public record DocumentView(
        long id,
        long knowledgeBaseId,
        String fileName,
        String contentType,
        long fileSize,
        String contentHash,
        DocumentStatus status,
        int chunkCount,
        String failureReason,
        String previewText
) {
}
