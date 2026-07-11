package know.studio.arag.evaluation.domain;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository {

    void insertDataset(EvaluationDataset dataset);

    Optional<EvaluationDataset> findDataset(long workspaceId, long datasetId);

    void insertSample(EvaluationSample sample);

    List<EvaluationSample> findSamples(long workspaceId, long datasetId);

    void insertRun(EvaluationRun run);
}
