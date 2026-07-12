package know.studio.arag.evaluation.domain;

import know.studio.arag.evaluation.api.EvaluationReport;
import know.studio.arag.identity.api.CurrentIdentity;
import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.identity.api.SystemRole;
import know.studio.arag.identity.api.WorkspaceRole;
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
                Instant.now()
        ));
        EvaluationService service = new EvaluationService(
                repository,
                query -> new EvidenceBundle(
                        query.mode() == RetrievalMode.VECTOR_ONLY
                                ? List.of(evidence(100L))
                                : List.of(evidence(100L), evidence(200L)),
                        EvidenceLevel.SUFFICIENT,
                        "ok"
                ),
                new StubIdentityApi(),
                new SnowflakeIdGenerator(0, 0)
        );

        EvaluationReport report = service.runAblation(10L, 1L, 5);

        assertThat(report.metrics()).extracting(metric -> metric.mode())
                .containsExactly(RetrievalMode.VECTOR_ONLY, RetrievalMode.HYBRID, RetrievalMode.HYBRID_RERANK);
        assertThat(report.metrics()).extracting(metric -> metric.recallAtK())
                .containsExactly(0.5, 1.0, 1.0);
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

        @Override
        public WorkspaceRole requireWorkspaceReadable(long workspaceId) {
            return WorkspaceRole.MEMBER;
        }

        @Override
        public WorkspaceRole requireRole(long workspaceId, WorkspaceRole requiredRole) {
            return WorkspaceRole.OWNER;
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
        public List<EvaluationDataset> findDatasets(long workspaceId) {
            return dataset != null && dataset.workspaceId() == workspaceId ? List.of(dataset) : List.of();
        }

        @Override
        public Optional<EvaluationDataset> findDataset(long workspaceId, long datasetId) {
            return dataset != null && dataset.workspaceId() == workspaceId && dataset.id() == datasetId
                    ? Optional.of(dataset)
                    : Optional.empty();
        }

        @Override
        public void insertSample(EvaluationSample sample) {
            samples.add(sample);
        }

        @Override
        public List<EvaluationSample> findSamples(long workspaceId, long datasetId) {
            return List.copyOf(samples);
        }

        @Override
        public void insertRun(EvaluationRun run) {
            runs.add(run);
        }

        @Override
        public List<EvaluationRun> findRuns(long workspaceId, long datasetId) {
            return runs.stream()
                    .filter(run -> run.workspaceId() == workspaceId && run.datasetId() == datasetId)
                    .toList();
        }
    }
}
