package know.studio.eval.domain;

import know.studio.eval.api.AddSampleCommand;
import know.studio.eval.api.CreateDatasetCommand;
import know.studio.eval.api.DatasetInfo;
import know.studio.eval.api.EvaluationApi;
import know.studio.eval.api.EvaluationMetric;
import know.studio.eval.api.EvaluationReport;
import know.studio.eval.api.EvaluationRunInfo;
import know.studio.eval.api.SampleInfo;
import know.studio.auth.api.CurrentIdentity;
import know.studio.auth.api.IdentityApi;
import know.studio.knowledge.api.KnowledgeAccessApi;
import know.studio.common.exception.BusinessException;
import know.studio.common.exception.ErrorCode;
import know.studio.common.id.SnowflakeIdGenerator;
import know.studio.common.trace.RagTraceNode;
import know.studio.search.api.Evidence;
import know.studio.search.api.EvidenceBundle;
import know.studio.search.api.EvidenceLevel;
import know.studio.search.api.RetrievalApi;
import know.studio.search.api.RetrievalMode;
import know.studio.search.api.RetrievalQuery;
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
    private final KnowledgeAccessApi knowledgeAccessApi;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional(readOnly = true)
    public List<DatasetInfo> listDatasets(long knowledgeBaseId) {
        knowledgeAccessApi.requireManageable(knowledgeBaseId);
        return repository.findDatasets(knowledgeBaseId).stream()
                .map(EvaluationService::toInfo)
                .toList();
    }

    @Override
    @Transactional
    public DatasetInfo createDataset(CreateDatasetCommand command) {
        knowledgeAccessApi.requireManageable(command.knowledgeBaseId());
        CurrentIdentity identity = identityApi.currentUser();
        String name = requireText(command.name(), "评测集名称不能为空");
        Instant now = Instant.now();
        EvaluationDataset dataset = new EvaluationDataset(
                idGenerator.nextId(),
                command.knowledgeBaseId(),
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
        requireDataset(command.knowledgeBaseId(), command.datasetId());
        String question = requireText(command.question(), "评测问题不能为空");
        List<Long> relevantIds = command.relevantChunkIds().stream().distinct().toList();
        if (relevantIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "相关 chunk ID 必须为正数");
        }
        if (command.expectRefusal() && !relevantIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "拒答样本不能包含相关 chunk ID");
        }
        if (!command.expectRefusal() && relevantIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "至少需要一个相关 chunk ID");
        }
        EvaluationSample sample = new EvaluationSample(
                idGenerator.nextId(),
                command.knowledgeBaseId(),
                command.datasetId(),
                question,
                relevantIds,
                normalize(command.expectedAnswer()),
                command.expectRefusal(),
                Instant.now()
        );
        repository.insertSample(sample);
        return toInfo(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleInfo> listSamples(long knowledgeBaseId, long datasetId) {
        requireDataset(knowledgeBaseId, datasetId);
        return repository.findSamples(knowledgeBaseId, datasetId).stream()
                .map(EvaluationService::toInfo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationRunInfo> listRuns(long knowledgeBaseId, long datasetId) {
        requireDataset(knowledgeBaseId, datasetId);
        return repository.findRuns(knowledgeBaseId, datasetId).stream()
                .map(EvaluationService::toInfo)
                .toList();
    }

    @Override
    @RagTraceNode("evaluation.ablation")
    public EvaluationReport runAblation(long knowledgeBaseId, long datasetId, int topK) {
        requireDataset(knowledgeBaseId, datasetId);
        if (topK < 1 || topK > 20) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "topK 必须在 1 到 20 之间");
        }
        List<EvaluationSample> samples = repository.findSamples(knowledgeBaseId, datasetId);
        if (samples.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评测集没有样本");
        }
        CurrentIdentity identity = identityApi.currentUser();
        List<EvaluationMetric> metrics = new ArrayList<>();
        for (RetrievalMode mode : EnumSet.allOf(RetrievalMode.class)) {
            EvaluationMetric metric = evaluate(knowledgeBaseId, samples, mode, topK);
            metrics.add(metric);
            repository.insertRun(new EvaluationRun(
                    idGenerator.nextId(),
                    knowledgeBaseId,
                    datasetId,
                    identity.userId(),
                    mode,
                    metric.recallAtK(),
                    metric.refusalAccuracy(),
                    metric.sampleCount(),
                    metric.positiveSampleCount(),
                    metric.refusalSampleCount(),
                    metric.averageLatencyMillis(),
                    topK,
                    Instant.now()
            ));
        }
        return new EvaluationReport(datasetId, topK, metrics, Instant.now());
    }

    private EvaluationMetric evaluate(
            long knowledgeBaseId,
            List<EvaluationSample> samples,
            RetrievalMode mode,
            int topK
    ) {
        double recallSum = 0;
        int positiveSampleCount = 0;
        int refusalSampleCount = 0;
        int correctRefusalCount = 0;
        long latencyNanos = 0;
        for (EvaluationSample sample : samples) {
            long start = System.nanoTime();
            EvidenceBundle result = retrievalApi.retrieve(new RetrievalQuery(
                    sample.question(),
                    Set.of(knowledgeBaseId),
                    topK,
                    mode
            ));
            latencyNanos += System.nanoTime() - start;
            if (sample.expectRefusal()) {
                refusalSampleCount++;
                if (result.level() == EvidenceLevel.NONE) {
                    correctRefusalCount++;
                }
                continue;
            }
            positiveSampleCount++;
            Set<Long> retrieved = new HashSet<>(result.evidence().stream().map(Evidence::chunkId).toList());
            long hits = sample.relevantChunkIds().stream().filter(retrieved::contains).count();
            recallSum += (double) hits / sample.relevantChunkIds().size();
        }
        return new EvaluationMetric(
                mode,
                positiveSampleCount == 0 ? 0.0 : recallSum / positiveSampleCount,
                refusalSampleCount == 0 ? 0.0 : (double) correctRefusalCount / refusalSampleCount,
                samples.size(),
                positiveSampleCount,
                refusalSampleCount,
                TimeUnit.NANOSECONDS.toMillis(latencyNanos) / samples.size()
        );
    }

    private EvaluationDataset requireDataset(long knowledgeBaseId, long datasetId) {
        knowledgeAccessApi.requireManageable(knowledgeBaseId);
        return repository.findDataset(knowledgeBaseId, datasetId)
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
                dataset.knowledgeBaseId(),
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
                sample.expectRefusal(),
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
                run.refusalAccuracy(),
                run.sampleCount(),
                run.positiveSampleCount(),
                run.refusalSampleCount(),
                run.averageLatencyMillis(),
                run.topK(),
                run.createdAt()
        );
    }
}
