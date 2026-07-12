package know.studio.arag.evaluation.api;

public interface EvaluationApi {

    java.util.List<DatasetInfo> listDatasets(long knowledgeBaseId);

    DatasetInfo createDataset(CreateDatasetCommand command);

    SampleInfo addSample(AddSampleCommand command);

    java.util.List<SampleInfo> listSamples(long knowledgeBaseId, long datasetId);

    java.util.List<EvaluationRunInfo> listRuns(long knowledgeBaseId, long datasetId);

    EvaluationReport runAblation(long knowledgeBaseId, long datasetId, int topK);
}
