package know.studio.arag.knowledge.domain;

import java.util.List;

public interface DocumentIndexPort {

    void replace(long knowledgeBaseId, DocumentRecord document, List<DocumentChunk> chunks, List<float[]> embeddings);

    void delete(long knowledgeBaseId, long documentId);
}
