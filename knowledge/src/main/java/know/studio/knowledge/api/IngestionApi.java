package know.studio.knowledge.api;

public interface IngestionApi {

    void submit(long knowledgeBaseId, long documentId);

    DocumentStatus status(long knowledgeBaseId, long documentId);
}
