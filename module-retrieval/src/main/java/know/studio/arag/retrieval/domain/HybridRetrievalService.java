package know.studio.arag.retrieval.domain;

import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.platform.ai.embedding.EmbeddingClient;
import know.studio.arag.platform.core.trace.RagTraceNode;
import know.studio.arag.retrieval.api.Evidence;
import know.studio.arag.retrieval.api.EvidenceBundle;
import know.studio.arag.retrieval.api.EvidenceLevel;
import know.studio.arag.retrieval.api.RetrievalApi;
import know.studio.arag.retrieval.api.RetrievalQuery;
import know.studio.arag.retrieval.api.RetrievalMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridRetrievalService implements RetrievalApi {

    private static final int CHANNEL_LIMIT = 50;
    private static final int RERANK_LIMIT = 20;

    private final IdentityApi identityApi;
    private final QueryPlanner queryPlanner;
    private final EmbeddingClient embeddingClient;
    private final VectorSearchPort vectorSearch;
    private final KeywordSearchPort keywordSearch;
    private final RrfFusion rrfFusion;
    private final ChunkNeighborPort neighborPort;
    private final NeighborExpander neighborExpander;
    private final CandidateClusterer candidateClusterer;
    private final RerankPort rerankPort;
    private final EvidenceGrader evidenceGrader;
    private final ExecutorService retrievalExecutor;

    @Override
    @RagTraceNode("retrieval.hybrid")
    public EvidenceBundle retrieve(RetrievalQuery query) {
        identityApi.requireWorkspaceReadable(query.workspaceId());
        List<String> plannedQueries = queryPlanner.plan(query.question());
        List<float[]> embeddings = embeddingClient.embed(plannedQueries);
        if (embeddings.size() != plannedQueries.size()) {
            throw new IllegalStateException("Query embedding count does not match planned query count");
        }

        List<CompletableFuture<List<SearchCandidate>>> searches = new ArrayList<>();
        for (int index = 0; index < plannedQueries.size(); index++) {
            String plannedQuery = plannedQueries.get(index);
            float[] embedding = embeddings.get(index);
            searches.add(searchAsync(
                    "vector",
                    () -> vectorSearch.search(query.workspaceId(), embedding, CHANNEL_LIMIT)
            ));
            if (query.mode() != RetrievalMode.VECTOR_ONLY) {
                searches.add(searchAsync(
                        "keyword",
                        () -> keywordSearch.search(query.workspaceId(), plannedQuery, CHANNEL_LIMIT)
                ));
            }
        }

        List<List<SearchCandidate>> rankings = searches.stream()
                .map(CompletableFuture::join)
                .filter(ranking -> !ranking.isEmpty())
                .toList();
        List<FusedCandidate> fused = rrfFusion.fuse(rankings, RERANK_LIMIT);
        List<NeighborChunk> neighbors = fused.isEmpty()
                ? List.of()
                : neighborPort.findNeighbors(
                        query.workspaceId(),
                        fused.stream().map(FusedCandidate::chunkId).toList(),
                        1
                );
        List<FusedCandidate> expanded = neighborExpander.expand(fused, neighbors, RERANK_LIMIT);
        List<FusedCandidate> clustered = candidateClusterer.cluster(expanded, RERANK_LIMIT);
        List<FusedCandidate> ranked = query.mode() == RetrievalMode.HYBRID_RERANK
                ? rerank(query.question(), clustered)
                : clustered;
        EvidenceLevel level = evidenceGrader.grade(ranked);
        List<Evidence> evidence = ranked.stream()
                .limit(query.topK())
                .map(HybridRetrievalService::toEvidence)
                .toList();
        return new EvidenceBundle(evidence, level, guidance(level));
    }

    private CompletableFuture<List<SearchCandidate>> searchAsync(
            String channel,
            Supplier<List<SearchCandidate>> search
    ) {
        return CompletableFuture.supplyAsync(search, retrievalExecutor)
                .exceptionally(exception -> {
                    log.warn("Retrieval channel failed channel={}", channel, exception);
                    return List.of();
                });
    }

    private List<FusedCandidate> rerank(String query, List<FusedCandidate> candidates) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        try {
            List<FusedCandidate> reranked = rerankPort.rerank(query, candidates);
            return reranked.isEmpty() ? candidates : reranked;
        } catch (RuntimeException exception) {
            log.warn("Rerank unavailable, using RRF order candidateCount={}", candidates.size(), exception);
            return candidates;
        }
    }

    private static Evidence toEvidence(FusedCandidate candidate) {
        return new Evidence(
                candidate.documentId(),
                candidate.chunkId(),
                candidate.chunkIndex(),
                candidate.fileName(),
                candidate.text(),
                candidate.finalScore(),
                candidate.sources().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet())
        );
    }

    private static String guidance(EvidenceLevel level) {
        return switch (level) {
            case SUFFICIENT -> "证据充分，可基于引用内容回答";
            case PARTIAL -> "证据部分充分，回答时说明不确定范围";
            case WEAK -> "证据较弱，仅提供保守结论并建议补充信息";
            case NONE -> "没有可靠证据，应拒绝编造并请求澄清";
        };
    }
}
