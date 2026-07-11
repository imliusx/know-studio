package know.studio.arag.evaluation.api;

public interface EvaluationApi {

    java.util.List<DatasetInfo> listDatasets(long workspaceId);

    DatasetInfo createDataset(CreateDatasetCommand command);

    SampleInfo addSample(AddSampleCommand command);

    java.util.List<SampleInfo> listSamples(long workspaceId, long datasetId);

    java.util.List<EvaluationRunInfo> listRuns(long workspaceId, long datasetId);

    EvaluationReport runAblation(long workspaceId, long datasetId, int topK);
}
