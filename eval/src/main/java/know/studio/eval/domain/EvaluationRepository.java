package know.studio.eval.domain;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository {

    void insertDataset(EvaluationDataset dataset);

    List<EvaluationDataset> findDatasets(long knowledgeBaseId);

    Optional<EvaluationDataset> findDataset(long knowledgeBaseId, long datasetId);

    void insertSample(EvaluationSample sample);

    List<EvaluationSample> findSamples(long knowledgeBaseId, long datasetId);

    void insertRun(EvaluationRun run);

    List<EvaluationRun> findRuns(long knowledgeBaseId, long datasetId);
}
