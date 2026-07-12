package know.studio.arag.evaluation.domain;

import know.studio.arag.evaluation.api.AddSampleCommand;
import know.studio.arag.evaluation.api.CreateDatasetCommand;
import know.studio.arag.evaluation.api.DatasetInfo;
import know.studio.arag.evaluation.api.EvaluationApi;
import know.studio.arag.evaluation.api.EvaluationMetric;
import know.studio.arag.evaluation.api.EvaluationReport;
import know.studio.arag.evaluation.api.EvaluationRunInfo;
import know.studio.arag.evaluation.api.SampleInfo;
import know.studio.arag.identity.api.CurrentIdentity;
import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.identity.api.WorkspaceRole;
import know.studio.arag.platform.core.exception.BusinessException;
import know.studio.arag.platform.core.exception.ErrorCode;
import know.studio.arag.platform.core.id.SnowflakeIdGenerator;
import know.studio.arag.platform.core.trace.RagTraceNode;
import know.studio.arag.retrieval.api.Evidence;
import know.studio.arag.retrieval.api.EvidenceBundle;
import know.studio.arag.retrieval.api.RetrievalApi;
import know.studio.arag.retrieval.api.RetrievalMode;
import know.studio.arag.retrieval.api.RetrievalQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EvaluationService implements EvaluationApi {

    private final EvaluationRepository repository;
    private final RetrievalApi retrievalApi;
    private final IdentityApi identityApi;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional(readOnly = true)
    public List<DatasetInfo> listDatasets(long workspaceId) {
        identityApi.requireRole(workspaceId, WorkspaceRole.ADMIN);
        return repository.findDatasets(workspaceId).stream()
                .map(EvaluationService::toInfo)
                .toList();
    }

    @Override
    @Transactional
    public DatasetInfo createDataset(CreateDatasetCommand command) {
        identityApi.requireRole(command.workspaceId(), WorkspaceRole.ADMIN);
        CurrentIdentity identity = identityApi.currentUser();
        String name = requireText(command.name(), "评测集名称不能为空");
        Instant now = Instant.now();
        EvaluationDataset dataset = new EvaluationDataset(
                idGenerator.nextId(),
                command.workspaceId(),
                identity.userId(),
                name,
                normalize(command.description()),
                "ACTIVE",
                now,
                now
        );
        try {
            repository.insertDataset(dataset);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "同名评测集已存在");
        }
        return toInfo(dataset);
    }

    @Override
    @Transactional
    public SampleInfo addSample(AddSampleCommand command) {
        requireDataset(command.workspaceId(), command.datasetId());
        String question = requireText(command.question(), "评测问题不能为空");
        List<Long> relevantIds = command.relevantChunkIds().stream().distinct().toList();
        if (relevantIds.isEmpty() || relevantIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "至少需要一个相关 chunk ID");
        }
        EvaluationSample sample = new EvaluationSample(
                idGenerator.nextId(),
                command.workspaceId(),
                command.datasetId(),
                question,
                relevantIds,
                normalize(command.expectedAnswer()),
                Instant.now()
        );
        repository.insertSample(sample);
        return toInfo(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleInfo> listSamples(long workspaceId, long datasetId) {
        requireDataset(workspaceId, datasetId);
        return repository.findSamples(workspaceId, datasetId).stream()
                .map(EvaluationService::toInfo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationRunInfo> listRuns(long workspaceId, long datasetId) {
        requireDataset(workspaceId, datasetId);
        return repository.findRuns(workspaceId, datasetId).stream()
                .map(EvaluationService::toInfo)
                .toList();
    }

    @Override
    @RagTraceNode("evaluation.ablation")
    public EvaluationReport runAblation(long workspaceId, long datasetId, int topK) {
        requireDataset(workspaceId, datasetId);
        if (topK < 1 || topK > 20) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "topK 必须在 1 到 20 之间");
        }
        List<EvaluationSample> samples = repository.findSamples(workspaceId, datasetId);
        if (samples.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评测集没有样本");
        }
        CurrentIdentity identity = identityApi.currentUser();
        List<EvaluationMetric> metrics = new ArrayList<>();
        for (RetrievalMode mode : EnumSet.allOf(RetrievalMode.class)) {
            EvaluationMetric metric = evaluate(workspaceId, samples, mode, topK);
            metrics.add(metric);
            repository.insertRun(new EvaluationRun(
                    idGenerator.nextId(),
                    workspaceId,
                    datasetId,
                    identity.userId(),
                    mode,
                    metric.recallAtK(),
                    metric.sampleCount(),
                    metric.averageLatencyMillis(),
                    topK,
                    Instant.now()
            ));
        }
        return new EvaluationReport(datasetId, topK, metrics, Instant.now());
    }

    private EvaluationMetric evaluate(
            long workspaceId,
            List<EvaluationSample> samples,
            RetrievalMode mode,
            int topK
    ) {
        double recallSum = 0;
        long latencyNanos = 0;
        for (EvaluationSample sample : samples) {
            long start = System.nanoTime();
            EvidenceBundle result = retrievalApi.retrieve(new RetrievalQuery(
                    sample.question(),
                    Set.of(workspaceId),
                    topK,
                    mode
            ));
            latencyNanos += System.nanoTime() - start;
            Set<Long> retrieved = new HashSet<>(result.evidence().stream().map(Evidence::chunkId).toList());
            long hits = sample.relevantChunkIds().stream().filter(retrieved::contains).count();
            recallSum += (double) hits / sample.relevantChunkIds().size();
        }
        return new EvaluationMetric(
                mode,
                recallSum / samples.size(),
                samples.size(),
                TimeUnit.NANOSECONDS.toMillis(latencyNanos) / samples.size()
        );
    }

    private EvaluationDataset requireDataset(long workspaceId, long datasetId) {
        identityApi.requireRole(workspaceId, WorkspaceRole.ADMIN);
        return repository.findDataset(workspaceId, datasetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评测集不存在"));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static DatasetInfo toInfo(EvaluationDataset dataset) {
        return new DatasetInfo(
                dataset.id(),
                dataset.workspaceId(),
                dataset.userId(),
                dataset.name(),
                dataset.description(),
                dataset.createdAt()
        );
    }

    private static SampleInfo toInfo(EvaluationSample sample) {
        return new SampleInfo(
                sample.id(),
                sample.datasetId(),
                sample.question(),
                sample.relevantChunkIds(),
                sample.expectedAnswer(),
                sample.createdAt()
        );
    }

    private static EvaluationRunInfo toInfo(EvaluationRun run) {
        return new EvaluationRunInfo(
                run.id(),
                run.datasetId(),
                run.userId(),
                run.mode(),
                run.recallAtK(),
                run.sampleCount(),
                run.averageLatencyMillis(),
                run.topK(),
                run.createdAt()
        );
    }
}
