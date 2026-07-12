package know.studio.arag.evaluation.domain;

import know.studio.arag.evaluation.api.EvaluationReport;
import know.studio.arag.identity.api.CurrentIdentity;
import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.identity.api.SystemRole;
import know.studio.arag.knowledge.api.KnowledgeAccessApi;
import know.studio.arag.knowledge.api.KnowledgeBaseInfo;
import know.studio.arag.knowledge.api.KnowledgeBasePermission;
import know.studio.arag.knowledge.api.KnowledgeBaseVisibility;
import know.studio.arag.platform.core.id.SnowflakeIdGenerator;
import know.studio.arag.retrieval.api.Evidence;
import know.studio.arag.retrieval.api.EvidenceBundle;
import know.studio.arag.retrieval.api.EvidenceLevel;
import know.studio.arag.retrieval.api.RetrievalMode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationServiceTest {

    @Test
    void runsThreeRealRetrievalModesAndComputesRecall() {
        InMemoryRepository repository = new InMemoryRepository();
        repository.dataset = new EvaluationDataset(1L, 10L, 20L, "set", null, "ACTIVE", Instant.now(), Instant.now());
        repository.samples.add(new EvaluationSample(
                2L,
                10L,
                1L,
                "question",
                List.of(100L, 200L),
                null,
                false,
                Instant.now()
        ));
        repository.samples.add(new EvaluationSample(
                3L,
                10L,
                1L,
                "unanswerable",
                List.of(),
                null,
                true,
                Instant.now()
        ));
        EvaluationService service = new EvaluationService(
                repository,
                query -> query.question().equals("unanswerable")
                        ? new EvidenceBundle(List.of(), EvidenceLevel.NONE, "refuse")
                        : new EvidenceBundle(
                                query.mode() == RetrievalMode.VECTOR_ONLY
                                        ? List.of(evidence(100L))
                                        : List.of(evidence(100L), evidence(200L)),
                                EvidenceLevel.SUFFICIENT,
                                "ok"
                        ),
                new StubIdentityApi(),
                new StubKnowledgeAccessApi(),
                new SnowflakeIdGenerator(0, 0)
        );

        EvaluationReport report = service.runAblation(10L, 1L, 5);

        assertThat(report.metrics()).extracting(metric -> metric.mode())
                .containsExactly(RetrievalMode.VECTOR_ONLY, RetrievalMode.HYBRID, RetrievalMode.HYBRID_RERANK);
        assertThat(report.metrics()).extracting(metric -> metric.recallAtK())
                .containsExactly(0.5, 1.0, 1.0);
        assertThat(report.metrics()).extracting(metric -> metric.refusalAccuracy())
                .containsOnly(1.0);
        assertThat(report.metrics()).extracting(metric -> metric.positiveSampleCount())
                .containsOnly(1);
        assertThat(report.metrics()).extracting(metric -> metric.refusalSampleCount())
                .containsOnly(1);
        assertThat(repository.runs).hasSize(3);
    }

    private static Evidence evidence(long chunkId) {
        return new Evidence(10L, 1L, chunkId, 0, "file", "text", 1.0, Set.of("VECTOR"));
    }

    private static final class StubIdentityApi implements IdentityApi {

        @Override
        public CurrentIdentity currentUser() {
            return new CurrentIdentity(20L, "user@example.com", "User", SystemRole.USER);
        }
    }

    private static final class StubKnowledgeAccessApi implements KnowledgeAccessApi {

        @Override
        public List<KnowledgeBaseInfo> listReadable() {
            return List.of(new KnowledgeBaseInfo(
                    10L,
                    "Evaluation Knowledge Base",
                    "Evaluation fixture",
                    KnowledgeBaseVisibility.PRIVATE,
                    null,
                    KnowledgeBasePermission.MANAGE
            ));
        }

        @Override
        public Set<Long> readableKnowledgeBaseIds() {
            return Set.of(10L);
        }

        @Override
        public KnowledgeBasePermission requireReadable(long knowledgeBaseId) {
            return KnowledgeBasePermission.MANAGE;
        }

        @Override
        public KnowledgeBasePermission requireManageable(long knowledgeBaseId) {
            return KnowledgeBasePermission.MANAGE;
        }
    }

    private static final class InMemoryRepository implements EvaluationRepository {

        private EvaluationDataset dataset;
        private final List<EvaluationSample> samples = new ArrayList<>();
        private final List<EvaluationRun> runs = new ArrayList<>();

        @Override
        public void insertDataset(EvaluationDataset value) {
            dataset = value;
        }

        @Override
        public List<EvaluationDataset> findDatasets(long knowledgeBaseId) {
            return dataset != null && dataset.knowledgeBaseId() == knowledgeBaseId ? List.of(dataset) : List.of();
        }

        @Override
        public Optional<EvaluationDataset> findDataset(long knowledgeBaseId, long datasetId) {
            return dataset != null && dataset.knowledgeBaseId() == knowledgeBaseId && dataset.id() == datasetId
                    ? Optional.of(dataset)
                    : Optional.empty();
        }

        @Override
        public void insertSample(EvaluationSample sample) {
            samples.add(sample);
        }

        @Override
        public List<EvaluationSample> findSamples(long knowledgeBaseId, long datasetId) {
            return List.copyOf(samples);
        }

        @Override
        public void insertRun(EvaluationRun run) {
            runs.add(run);
        }

        @Override
        public List<EvaluationRun> findRuns(long knowledgeBaseId, long datasetId) {
            return runs.stream()
                    .filter(run -> run.knowledgeBaseId() == knowledgeBaseId && run.datasetId() == datasetId)
                    .toList();
        }
    }
}
