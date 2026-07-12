package know.studio.arag.evaluation.api;

public record CreateDatasetCommand(long knowledgeBaseId, String name, String description) {
}
