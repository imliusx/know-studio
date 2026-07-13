package know.studio.knowledge.api;

import know.studio.common.json.JsonLongId;

public record DocumentView(
        @JsonLongId long id,
        @JsonLongId long knowledgeBaseId,
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
