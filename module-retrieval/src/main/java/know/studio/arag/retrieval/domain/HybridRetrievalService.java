package know.studio.arag.retrieval.domain;

import know.studio.arag.knowledge.api.KnowledgeAccessApi;
import know.studio.arag.knowledge.api.KnowledgeBaseInfo;
import know.studio.arag.platform.ai.embedding.EmbeddingClient;
import know.studio.arag.platform.core.exception.ForbiddenException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridRetrievalService implements RetrievalApi {

    private static final int CHANNEL_LIMIT = 50;
    private static final int RERANK_LIMIT = 20;
    private static final double SCOPE_CONFIDENCE_THRESHOLD = 0.75;

    private final KnowledgeAccessApi knowledgeAccessApi;
    private final KnowledgeBaseScopeSelector knowledgeBaseScopeSelector;
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
        RetrievalScope scope = authorizedScope(query.question(), query.knowledgeBaseIds());
        if (scope.noMatch()) {
            return new EvidenceBundle(List.of(), EvidenceLevel.NONE, guidance(EvidenceLevel.NONE));
        }
        Set<Long> knowledgeBaseIds = scope.knowledgeBaseIds();
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
                    () -> vectorSearch.search(knowledgeBaseIds, embedding, CHANNEL_LIMIT)
            ));
            if (query.mode() != RetrievalMode.VECTOR_ONLY) {
                searches.add(searchAsync(
                        "keyword",
                        () -> keywordSearch.search(knowledgeBaseIds, plannedQuery, CHANNEL_LIMIT)
                ));
            }
        }

        List<List<SearchCandidate>> rankings = searches.stream()
                .map(CompletableFuture::join)
                .filter(ranking -> !ranking.isEmpty())
                .toList();
        List<FusedCandidate> fused = rrfFusion.fuse(rankings, RERANK_LIMIT);
        List<FusedCandidate> rankedSeeds = query.mode() == RetrievalMode.HYBRID_RERANK
                ? rerank(query.question(), fused)
                : fused;
        List<NeighborChunk> neighbors = rankedSeeds.isEmpty()
                ? List.of()
                : neighborPort.findNeighbors(
                        knowledgeBaseIds,
                        rankedSeeds.stream().map(FusedCandidate::chunkId).toList(),
                        1
                );
        List<FusedCandidate> expanded = neighborExpander.expand(rankedSeeds, neighbors, RERANK_LIMIT);
        List<FusedCandidate> clustered = candidateClusterer.cluster(expanded, RERANK_LIMIT);
        EvidenceLevel level = evidenceGrader.grade(query.question(), clustered);
        List<Evidence> evidence = level == EvidenceLevel.NONE
                && query.mode() != RetrievalMode.VECTOR_ONLY
                ? List.of()
                : clustered.stream()
                        .limit(query.topK())
                        .map(HybridRetrievalService::toEvidence)
                        .toList();
        return new EvidenceBundle(evidence, level, guidance(level));
    }

    private RetrievalScope authorizedScope(String question, Set<Long> requestedIds) {
        List<KnowledgeBaseInfo> readableKnowledgeBases = knowledgeAccessApi.listReadable();
        Set<Long> authorizedIds = readableKnowledgeBases.stream()
                .map(KnowledgeBaseInfo::knowledgeBaseId)
                .collect(java.util.stream.Collectors.toSet());
        if (authorizedIds.isEmpty()) {
            throw new ForbiddenException("没有可用于检索的知识库");
        }
        Set<Long> effectiveIds = new HashSet<>(authorizedIds);
        if (!requestedIds.isEmpty()) {
            effectiveIds.retainAll(requestedIds);
            if (effectiveIds.isEmpty()) {
                throw new ForbiddenException("没有可用于检索的知识库");
            }
            return new RetrievalScope(Set.copyOf(effectiveIds), false);
        }
        if (readableKnowledgeBases.size() == 1) {
            return new RetrievalScope(Set.copyOf(effectiveIds), false);
        }

        KnowledgeBaseScopeDecision decision = knowledgeBaseScopeSelector.select(
                question,
                readableKnowledgeBases
        );
        log.info(
                "KnowledgeBase route confidence={} selectedCount={} authorizedCount={}",
                decision.confidence(),
                decision.knowledgeBaseIds().size(),
                authorizedIds.size()
        );
        if (decision.confidence() < SCOPE_CONFIDENCE_THRESHOLD) {
            return new RetrievalScope(Set.copyOf(effectiveIds), false);
        }
        effectiveIds.retainAll(decision.knowledgeBaseIds());
        return new RetrievalScope(Set.copyOf(effectiveIds), effectiveIds.isEmpty());
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
                candidate.knowledgeBaseId(),
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
            case NONE -> "当前知识库中没有找到与该问题相关的可靠资料，无法依据现有文档回答。";
        };
    }

    private record RetrievalScope(Set<Long> knowledgeBaseIds, boolean noMatch) {
    }
}
