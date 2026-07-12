package know.studio.arag.knowledge.domain;

public record DocumentChunk(
        long id,
        long knowledgeBaseId,
        long documentId,
        int chunkIndex,
        String text,
        int charStart,
        int charEnd,
        String sectionPath
) {
}
