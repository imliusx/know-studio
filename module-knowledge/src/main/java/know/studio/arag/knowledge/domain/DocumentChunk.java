package know.studio.arag.knowledge.domain;

public record DocumentChunk(
        long id,
        long workspaceId,
        long documentId,
        int chunkIndex,
        String text,
        int charStart,
        int charEnd,
        String sectionPath
) {
}
