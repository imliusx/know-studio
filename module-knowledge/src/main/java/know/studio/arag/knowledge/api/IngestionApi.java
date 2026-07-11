package know.studio.arag.knowledge.api;

public interface IngestionApi {

    void submit(long workspaceId, long documentId);

    DocumentStatus status(long workspaceId, long documentId);
}
