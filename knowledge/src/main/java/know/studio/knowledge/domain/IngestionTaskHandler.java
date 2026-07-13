package know.studio.knowledge.domain;

@FunctionalInterface
public interface IngestionTaskHandler {

    void process(long knowledgeBaseId, long documentId);
}
