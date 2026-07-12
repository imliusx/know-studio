package know.studio.arag.evaluation.infra.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import know.studio.arag.evaluation.domain.EvaluationDataset;
import know.studio.arag.evaluation.domain.EvaluationRepository;
import know.studio.arag.evaluation.domain.EvaluationRun;
import know.studio.arag.evaluation.domain.EvaluationSample;
import know.studio.arag.evaluation.infra.persistence.entity.EvaluationDatasetEntity;
import know.studio.arag.evaluation.infra.persistence.entity.EvaluationRunEntity;
import know.studio.arag.evaluation.infra.persistence.entity.EvaluationSampleEntity;
import know.studio.arag.evaluation.infra.persistence.mapper.EvaluationDatasetMapper;
import know.studio.arag.evaluation.infra.persistence.mapper.EvaluationRunMapper;
import know.studio.arag.evaluation.infra.persistence.mapper.EvaluationSampleMapper;
import know.studio.arag.platform.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisEvaluationRepository implements EvaluationRepository {

    private static final TypeReference<List<Long>> IDS_TYPE = new TypeReference<>() { };

    private final EvaluationDatasetMapper datasetMapper;
    private final EvaluationSampleMapper sampleMapper;
    private final EvaluationRunMapper runMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void insertDataset(EvaluationDataset dataset) {
        datasetMapper.insert(toEntity(dataset));
    }

    @Override
    public List<EvaluationDataset> findDatasets(long knowledgeBaseId) {
        return datasetMapper.selectList(Wrappers.<EvaluationDatasetEntity>lambdaQuery()
                        .eq(EvaluationDatasetEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(EvaluationDatasetEntity::getStatus, "ACTIVE")
                        .orderByDesc(EvaluationDatasetEntity::getCreatedAt))
                .stream()
                .map(MybatisEvaluationRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<EvaluationDataset> findDataset(long knowledgeBaseId, long datasetId) {
        EvaluationDatasetEntity entity = datasetMapper.selectOne(Wrappers.<EvaluationDatasetEntity>lambdaQuery()
                .eq(EvaluationDatasetEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(EvaluationDatasetEntity::getId, datasetId)
                .eq(EvaluationDatasetEntity::getStatus, "ACTIVE"));
        return Optional.ofNullable(entity).map(MybatisEvaluationRepository::toDomain);
    }

    @Override
    public void insertSample(EvaluationSample sample) {
        EvaluationSampleEntity entity = new EvaluationSampleEntity();
        entity.setId(sample.id());
        entity.setKnowledgeBaseId(sample.knowledgeBaseId());
        entity.setDatasetId(sample.datasetId());
        entity.setQuestion(sample.question());
        entity.setRelevantChunkIds(write(sample.relevantChunkIds()));
        entity.setExpectedAnswer(sample.expectedAnswer());
        entity.setCreatedAt(sample.createdAt());
        sampleMapper.insertJson(entity);
    }

    @Override
    public List<EvaluationSample> findSamples(long knowledgeBaseId, long datasetId) {
        return sampleMapper.selectOwned(knowledgeBaseId, datasetId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void insertRun(EvaluationRun run) {
        EvaluationRunEntity entity = new EvaluationRunEntity();
        entity.setId(run.id());
        entity.setKnowledgeBaseId(run.knowledgeBaseId());
        entity.setDatasetId(run.datasetId());
        entity.setUserId(run.userId());
        entity.setConfig(run.mode().name());
        entity.setRecallAtK(BigDecimal.valueOf(run.recallAtK()));
        entity.setSampleCount(run.sampleCount());
        entity.setAvgLatencyMs(run.averageLatencyMillis());
        entity.setExtra(write(java.util.Map.of("topK", run.topK())));
        entity.setCreatedAt(run.createdAt());
        runMapper.insertJson(entity);
    }

    @Override
    public List<EvaluationRun> findRuns(long knowledgeBaseId, long datasetId) {
        return runMapper.selectList(Wrappers.<EvaluationRunEntity>lambdaQuery()
                        .eq(EvaluationRunEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(EvaluationRunEntity::getDatasetId, datasetId)
                        .orderByDesc(EvaluationRunEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private static EvaluationDatasetEntity toEntity(EvaluationDataset dataset) {
        EvaluationDatasetEntity entity = new EvaluationDatasetEntity();
        entity.setId(dataset.id());
        entity.setKnowledgeBaseId(dataset.knowledgeBaseId());
        entity.setUserId(dataset.userId());
        entity.setName(dataset.name());
        entity.setDescription(dataset.description());
        entity.setStatus(dataset.status());
        entity.setCreatedAt(dataset.createdAt());
        entity.setUpdatedAt(dataset.updatedAt());
        return entity;
    }

    private static EvaluationDataset toDomain(EvaluationDatasetEntity entity) {
        return new EvaluationDataset(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getUserId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private EvaluationSample toDomain(EvaluationSampleEntity entity) {
        return new EvaluationSample(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getDatasetId(),
                entity.getQuestion(),
                readIds(entity.getRelevantChunkIds()),
                entity.getExpectedAnswer(),
                entity.getCreatedAt()
        );
    }

    private EvaluationRun toDomain(EvaluationRunEntity entity) {
        return new EvaluationRun(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getDatasetId(),
                entity.getUserId(),
                know.studio.arag.retrieval.api.RetrievalMode.valueOf(entity.getConfig()),
                entity.getRecallAtK().doubleValue(),
                entity.getSampleCount(),
                entity.getAvgLatencyMs(),
                readTopK(entity.getExtra()),
                entity.getCreatedAt()
        );
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("评测 JSON 无法序列化");
        }
    }

    private List<Long> readIds(String value) {
        try {
            return objectMapper.readValue(value, IDS_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("相关 chunk ID 无法解析");
        }
    }

    private int readTopK(String value) {
        try {
            Map<?, ?> extra = objectMapper.readValue(value, Map.class);
            Object topK = extra.get("topK");
            return topK instanceof Number number ? number.intValue() : 0;
        } catch (JsonProcessingException exception) {
            throw new BusinessException("评测运行参数无法解析");
        }
    }
}
