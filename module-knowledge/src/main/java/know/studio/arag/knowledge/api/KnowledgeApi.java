package know.studio.arag.knowledge.api;

public interface KnowledgeApi {

    DocumentView getDocument(long workspaceId, long documentId);

    java.util.List<DocumentView> listDocuments(long workspaceId, DocumentStatus status, String fileName);

    void deleteDocument(long workspaceId, long documentId);

    void retryIngestion(long workspaceId, long documentId);
}
