package know.studio.arag.retrieval.domain;

import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.platform.ai.embedding.EmbeddingClient;
import know.studio.arag.platform.ai.provider.AiCapability;
import know.studio.arag.platform.ai.provider.AiProvider;
import know.studio.arag.platform.ai.routing.CircuitBreakerConfig;
import know.studio.arag.platform.ai.routing.ProviderCircuitBreakerRegistry;
import know.studio.arag.platform.core.exception.ForbiddenException;
import know.studio.arag.retrieval.api.EvidenceBundle;
import know.studio.arag.retrieval.api.EvidenceLevel;
import know.studio.arag.retrieval.api.RetrievalQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridRetrievalServiceTest {

    private static final long WORKSPACE_ID = 11L;

    @Mock
    private IdentityApi identityApi;
    @Mock
    private QueryPlanner queryPlanner;
    @Mock
    private VectorSearchPort vectorSearch;
    @Mock
    private KeywordSearchPort keywordSearch;
    @Mock
    private RerankPort rerankPort;
    @Mock
    private ChunkNeighborPort neighborPort;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void fusesVectorAndKeywordEvidenceAndFallsBackWhenRerankFails() {
        when(queryPlanner.plan("What is RAG?")).thenReturn(List.of("What is RAG?"));
        when(vectorSearch.search(anyLong(), any(float[].class), anyInt()))
                .thenReturn(List.of(candidate(1L, 0.91, RetrievalSource.VECTOR)));
        when(keywordSearch.search(WORKSPACE_ID, "What is RAG?", 50))
                .thenReturn(List.of(candidate(1L, 8.2, RetrievalSource.KEYWORD)));
        when(rerankPort.rerank(any(), any())).thenThrow(new IllegalStateException("reranker offline"));
        when(neighborPort.findNeighbors(anyLong(), any(), anyInt())).thenReturn(List.of());

        EvidenceBundle result = service().retrieve(new RetrievalQuery("What is RAG?", WORKSPACE_ID, 5));

        assertThat(result.level()).isEqualTo(EvidenceLevel.PARTIAL);
        assertThat(result.evidence()).hasSize(1);
        assertThat(result.evidence().getFirst().sources()).containsExactlyInAnyOrder("VECTOR", "KEYWORD");
    }

    @Test
    void continuesWithVectorEvidenceWhenKeywordChannelFails() {
        when(queryPlanner.plan("retrieval")).thenReturn(List.of("retrieval"));
        when(vectorSearch.search(anyLong(), any(float[].class), anyInt())).thenReturn(List.of(
                candidate(1L, 0.91, RetrievalSource.VECTOR),
                candidate(2L, 0.80, RetrievalSource.VECTOR)
        ));
        when(keywordSearch.search(WORKSPACE_ID, "retrieval", 50))
                .thenThrow(new IllegalStateException("elasticsearch offline"));
        when(rerankPort.rerank(any(), any())).thenThrow(new IllegalStateException("reranker offline"));
        when(neighborPort.findNeighbors(anyLong(), any(), anyInt())).thenReturn(List.of());

        EvidenceBundle result = service().retrieve(new RetrievalQuery("retrieval", WORKSPACE_ID, 5));

        assertThat(result.level()).isEqualTo(EvidenceLevel.WEAK);
        assertThat(result.evidence()).hasSize(1);
        assertThat(result.evidence().getFirst().text())
                .contains("retrieval content 1", "retrieval content 2");
    }

    @Test
    void deniesWorkspaceBeforePlanningOrCallingModels() {
        doThrow(new ForbiddenException()).when(identityApi).requireWorkspaceReadable(WORKSPACE_ID);

        assertThatThrownBy(() -> service().retrieve(new RetrievalQuery("retrieval", WORKSPACE_ID, 5)))
                .isInstanceOf(ForbiddenException.class);

        verify(queryPlanner, never()).plan(any());
        verify(vectorSearch, never()).search(anyLong(), any(), anyInt());
    }

    private HybridRetrievalService service() {
        return new HybridRetrievalService(
                identityApi,
                queryPlanner,
                embeddingClient(),
                vectorSearch,
                keywordSearch,
                new RrfFusion(),
                neighborPort,
                new NeighborExpander(),
                new CandidateClusterer(),
                rerankPort,
                new EvidenceGrader(),
                executor
        );
    }

    private static EmbeddingClient embeddingClient() {
        AiProvider provider = new AiProvider() {
            @Override
            public String id() {
                return "embedding-test";
            }

            @Override
            public int priority() {
                return 1;
            }

            @Override
            public Set<AiCapability> capabilities() {
                return Set.of(AiCapability.EMBEDDING);
            }

            @Override
            public List<float[]> embed(List<String> texts) {
                return texts.stream().map(ignored -> new float[1_024]).toList();
            }
        };
        return new EmbeddingClient(
                List.of(provider),
                new ProviderCircuitBreakerRegistry(new CircuitBreakerConfig(3, Duration.ofSeconds(30)))
        );
    }

    private static SearchCandidate candidate(long chunkId, double score, RetrievalSource source) {
        return new SearchCandidate(
                chunkId,
                100L,
                Math.toIntExact(chunkId),
                "guide.md",
                "retrieval content " + chunkId,
                score,
                source
        );
    }
}
