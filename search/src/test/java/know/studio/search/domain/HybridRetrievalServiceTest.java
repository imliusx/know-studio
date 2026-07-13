package know.studio.search.domain;

import know.studio.knowledge.api.KnowledgeAccessApi;
import know.studio.knowledge.api.KnowledgeBaseInfo;
import know.studio.knowledge.api.KnowledgeBasePermission;
import know.studio.knowledge.api.KnowledgeBaseVisibility;
import know.studio.ai.embedding.EmbeddingClient;
import know.studio.ai.provider.AiCapability;
import know.studio.ai.provider.AiProvider;
import know.studio.ai.routing.CircuitBreakerConfig;
import know.studio.ai.routing.ProviderCircuitBreakerRegistry;
import know.studio.common.exception.ForbiddenException;
import know.studio.search.api.EvidenceBundle;
import know.studio.search.api.EvidenceLevel;
import know.studio.search.api.RetrievalQuery;
import know.studio.search.api.RetrievalMode;
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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridRetrievalServiceTest {

    private static final long KNOWLEDGE_BASE_ID = 11L;
    private static final String EXPENSE_QUESTION =
            "费用报销 客户拜访产生的交通、餐饮和住宿费用分别怎么报";

    @Mock
    private KnowledgeAccessApi knowledgeAccessApi;
    @Mock
    private KnowledgeBaseScopeSelector knowledgeBaseScopeSelector;
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
        when(knowledgeAccessApi.listReadable()).thenReturn(List.of(knowledgeBase(KNOWLEDGE_BASE_ID)));
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void fusesVectorAndKeywordEvidenceAndFallsBackWhenRerankFails() {
        when(queryPlanner.plan("What is retrieval?")).thenReturn(List.of("What is retrieval?"));
        when(vectorSearch.search(anySet(), any(float[].class), anyInt()))
                .thenReturn(List.of(candidate(1L, 0.91, RetrievalSource.VECTOR)));
        when(keywordSearch.search(Set.of(KNOWLEDGE_BASE_ID), "What is retrieval?", 50))
                .thenReturn(List.of(candidate(1L, 8.2, RetrievalSource.KEYWORD)));
        when(rerankPort.rerank(any(), any())).thenThrow(new IllegalStateException("reranker offline"));
        when(neighborPort.findNeighbors(anySet(), any(), anyInt())).thenReturn(List.of());

        EvidenceBundle result = service().retrieve(new RetrievalQuery("What is retrieval?", Set.of(), 5));

        assertThat(result.level()).isEqualTo(EvidenceLevel.PARTIAL);
        assertThat(result.evidence()).hasSize(1);
        assertThat(result.evidence().getFirst().sources()).containsExactlyInAnyOrder("VECTOR", "KEYWORD");
    }

    @Test
    void continuesWithVectorEvidenceWhenKeywordChannelFails() {
        when(queryPlanner.plan("retrieval")).thenReturn(List.of("retrieval"));
        when(vectorSearch.search(anySet(), any(float[].class), anyInt())).thenReturn(List.of(
                candidate(1L, 0.91, RetrievalSource.VECTOR),
                candidate(2L, 0.80, RetrievalSource.VECTOR)
        ));
        when(keywordSearch.search(Set.of(KNOWLEDGE_BASE_ID), "retrieval", 50))
                .thenThrow(new IllegalStateException("elasticsearch offline"));
        when(rerankPort.rerank(any(), any())).thenThrow(new IllegalStateException("reranker offline"));
        when(neighborPort.findNeighbors(anySet(), any(), anyInt())).thenReturn(List.of());

        EvidenceBundle result = service().retrieve(new RetrievalQuery("retrieval", Set.of(), 5));

        assertThat(result.level()).isEqualTo(EvidenceLevel.WEAK);
        assertThat(result.evidence()).hasSize(1);
        assertThat(result.evidence().getFirst().text())
                .contains("retrieval content 1", "retrieval content 2");
    }

    @Test
    void deniesRetrievalWithoutReadableKnowledgeBasesBeforePlanningOrCallingModels() {
        when(knowledgeAccessApi.listReadable()).thenReturn(List.of());

        assertThatThrownBy(() -> service().retrieve(new RetrievalQuery("retrieval", Set.of(), 5)))
                .isInstanceOf(ForbiddenException.class);

        verify(queryPlanner, never()).plan(any());
        verify(vectorSearch, never()).search(anySet(), any(), anyInt());
    }

    @Test
    void vectorOnlySkipsKeywordAndRerank() {
        when(queryPlanner.plan("vector only")).thenReturn(List.of("vector only"));
        when(vectorSearch.search(anySet(), any(float[].class), anyInt()))
                .thenReturn(List.of(candidate(1L, 0.91, RetrievalSource.VECTOR)));
        when(neighborPort.findNeighbors(anySet(), any(), anyInt())).thenReturn(List.of());

        EvidenceBundle result = service().retrieve(new RetrievalQuery(
                "vector only",
                Set.of(KNOWLEDGE_BASE_ID),
                5,
                RetrievalMode.VECTOR_ONLY
        ));

        assertThat(result.evidence()).hasSize(1);
        verify(keywordSearch, never()).search(anySet(), any(), anyInt());
        verify(rerankPort, never()).rerank(any(), any());
    }

    @Test
    void requestedScopeCanOnlyNarrowReadableKnowledgeBases() {
        when(knowledgeAccessApi.listReadable()).thenReturn(List.of(
                knowledgeBase(11L),
                knowledgeBase(12L)
        ));
        when(queryPlanner.plan("scoped")).thenReturn(List.of("scoped"));
        when(vectorSearch.search(eq(Set.of(12L)), any(float[].class), eq(50))).thenReturn(List.of());

        service().retrieve(new RetrievalQuery(
                "scoped",
                Set.of(12L, 99L),
                5,
                RetrievalMode.VECTOR_ONLY
        ));

        verify(vectorSearch).search(eq(Set.of(12L)), any(float[].class), eq(50));
        verify(knowledgeBaseScopeSelector, never()).select(any(), any());
    }

    @Test
    void routesUnscopedQuestionToHighConfidenceKnowledgeBaseSubset() {
        KnowledgeBaseInfo technical = knowledgeBase(KNOWLEDGE_BASE_ID);
        KnowledgeBaseInfo handbook = knowledgeBase(12L);
        when(knowledgeAccessApi.listReadable()).thenReturn(List.of(technical, handbook));
        when(knowledgeBaseScopeSelector.select("Java 类名如何命名？", List.of(technical, handbook)))
                .thenReturn(new KnowledgeBaseScopeDecision(Set.of(KNOWLEDGE_BASE_ID), 0.91));
        when(queryPlanner.plan("Java 类名如何命名？")).thenReturn(List.of("Java 类名如何命名"));
        when(vectorSearch.search(eq(Set.of(KNOWLEDGE_BASE_ID)), any(float[].class), eq(50)))
                .thenReturn(List.of());
        when(keywordSearch.search(Set.of(KNOWLEDGE_BASE_ID), "Java 类名如何命名", 50))
                .thenReturn(List.of());

        service().retrieve(new RetrievalQuery("Java 类名如何命名？", Set.of(), 5));

        verify(vectorSearch).search(eq(Set.of(KNOWLEDGE_BASE_ID)), any(float[].class), eq(50));
        verify(keywordSearch).search(Set.of(KNOWLEDGE_BASE_ID), "Java 类名如何命名", 50);
    }

    @Test
    void fallsBackToAuthorizedScopeWhenRoutingConfidenceIsLow() {
        KnowledgeBaseInfo technical = knowledgeBase(KNOWLEDGE_BASE_ID);
        KnowledgeBaseInfo handbook = knowledgeBase(12L);
        when(knowledgeAccessApi.listReadable()).thenReturn(List.of(technical, handbook));
        when(knowledgeBaseScopeSelector.select(EXPENSE_QUESTION, List.of(technical, handbook)))
                .thenReturn(KnowledgeBaseScopeDecision.uncertain());
        when(queryPlanner.plan(EXPENSE_QUESTION)).thenReturn(List.of(EXPENSE_QUESTION));
        when(vectorSearch.search(eq(Set.of(KNOWLEDGE_BASE_ID, 12L)), any(float[].class), eq(50)))
                .thenReturn(List.of());
        when(keywordSearch.search(Set.of(KNOWLEDGE_BASE_ID, 12L), EXPENSE_QUESTION, 50))
                .thenReturn(List.of());

        EvidenceBundle result = service().retrieve(new RetrievalQuery(
                EXPENSE_QUESTION,
                Set.of(),
                5
        ));

        assertThat(result.level()).isEqualTo(EvidenceLevel.NONE);
        assertThat(result.evidence()).isEmpty();
        verify(vectorSearch).search(eq(Set.of(KNOWLEDGE_BASE_ID, 12L)), any(), eq(50));
        verify(keywordSearch).search(Set.of(KNOWLEDGE_BASE_ID, 12L), EXPENSE_QUESTION, 50);
    }

    @Test
    void removesUnrelatedCandidatesFromNoneEvidenceBundle() {
        SearchCandidate unrelatedJavaRule = candidate(
                1L,
                "Java 类名使用 UpperCamelCase 风格，方法名、参数名和局部变量统一使用 lowerCamelCase 风格。",
                0.91,
                RetrievalSource.VECTOR
        );
        when(queryPlanner.plan(EXPENSE_QUESTION)).thenReturn(List.of(EXPENSE_QUESTION));
        when(vectorSearch.search(anySet(), any(float[].class), anyInt()))
                .thenReturn(List.of(unrelatedJavaRule));
        when(keywordSearch.search(
                Set.of(KNOWLEDGE_BASE_ID),
                EXPENSE_QUESTION,
                50
        )).thenReturn(List.of(candidate(
                1L,
                unrelatedJavaRule.text(),
                8.2,
                RetrievalSource.KEYWORD
        )));
        when(rerankPort.rerank(any(), any())).thenThrow(new IllegalStateException("reranker offline"));
        when(neighborPort.findNeighbors(anySet(), any(), anyInt())).thenReturn(List.of());

        EvidenceBundle result = service().retrieve(new RetrievalQuery(
                EXPENSE_QUESTION,
                Set.of(),
                5
        ));

        assertThat(result.level()).isEqualTo(EvidenceLevel.NONE);
        assertThat(result.evidence()).isEmpty();
    }

    private HybridRetrievalService service() {
        return new HybridRetrievalService(
                knowledgeAccessApi,
                knowledgeBaseScopeSelector,
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
        return candidate(chunkId, "retrieval content " + chunkId, score, source);
    }

    private static KnowledgeBaseInfo knowledgeBase(long knowledgeBaseId) {
        return new KnowledgeBaseInfo(
                knowledgeBaseId,
                "Knowledge Base " + knowledgeBaseId,
                "Test knowledge base",
                KnowledgeBaseVisibility.PRIVATE,
                null,
                KnowledgeBasePermission.MANAGE
        );
    }

    private static SearchCandidate candidate(
            long chunkId,
            String text,
            double score,
            RetrievalSource source
    ) {
        return new SearchCandidate(
                KNOWLEDGE_BASE_ID,
                chunkId,
                100L,
                Math.toIntExact(chunkId),
                "guide.md",
                text,
                score,
                source
        );
    }
}
