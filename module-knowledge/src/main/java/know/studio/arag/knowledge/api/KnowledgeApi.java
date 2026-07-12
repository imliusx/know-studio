package know.studio.arag.knowledge.api;

public interface KnowledgeApi {

    DocumentView getDocument(long knowledgeBaseId, long documentId);

    java.util.List<DocumentView> listDocuments(long knowledgeBaseId, DocumentStatus status, String fileName);

    void deleteDocument(long knowledgeBaseId, long documentId);

    void retryIngestion(long knowledgeBaseId, long documentId);
}
