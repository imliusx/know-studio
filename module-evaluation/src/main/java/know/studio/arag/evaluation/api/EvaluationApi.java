package know.studio.arag.evaluation.api;

public interface EvaluationApi {

    DatasetInfo createDataset(CreateDatasetCommand command);

    SampleInfo addSample(AddSampleCommand command);

    EvaluationReport runAblation(long workspaceId, long datasetId, int topK);
}
