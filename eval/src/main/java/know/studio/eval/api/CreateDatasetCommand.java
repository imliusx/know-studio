package know.studio.eval.api;

public record CreateDatasetCommand(long knowledgeBaseId, String name, String description) {
}
