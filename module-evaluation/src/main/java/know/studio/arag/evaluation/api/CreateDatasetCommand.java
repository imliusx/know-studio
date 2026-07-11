package know.studio.arag.evaluation.api;

public record CreateDatasetCommand(long workspaceId, String name, String description) {
}
