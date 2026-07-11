package know.studio.arag.knowledge.domain;

import java.util.List;

public interface DocumentIndexPort {

    void replace(long workspaceId, DocumentRecord document, List<DocumentChunk> chunks, List<float[]> embeddings);
}
