package know.studio.arag.knowledge.domain;

@FunctionalInterface
public interface IngestionTaskHandler {

    void process(long knowledgeBaseId, long documentId);
}
