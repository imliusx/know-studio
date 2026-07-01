package com.dong.ddrag.qa.rag;

import com.dong.ddrag.common.exception.BusinessException;
import com.dong.ddrag.ingestion.mapper.DocumentChunkMapper;
import com.dong.ddrag.ingestion.model.entity.DocumentChunkEntity;
import com.dong.ddrag.qa.model.EvidenceLevel;
import com.dong.ddrag.qa.model.QueryPlanResult;
import com.dong.ddrag.qa.service.QueryPlanningService;
import com.dong.ddrag.retrieval.elasticsearch.ElasticsearchChunkIndexService;
import com.dong.ddrag.retrieval.vectorstore.PgVectorRetrievalAdapter;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 混合检索引擎——整个 RAG 的"心脏"。对应走读指南「链路 B：QA 问答」。
 *
 * <p>核心方法 {@link #retrieve(Long, String, int)} 把"用户问题"加工成"带分级的证据包"，
 * 分 5 段流水线（每段都解决一个真实工程问题）：
 * <pre>
 *   ① 查询规划：一个问题改写成多个 query（DIRECT/REWRITE/DECOMPOSE）——撒更大的网
 *   ② 双通道召回：每个 query 同时跑 向量(pgvector) + 关键词(ES BM25)，各取 topK=50
 *   ③ RRF 融合：只比"排名"不比"分数"，合并两路结果——规避两路打分尺度不一致
 *   ④ 聚簇 + 邻居扩窗：同文档相邻切片合并成一条证据，前后各扩 1 片补全上下文
 *   ⑤ 证据分级：按证据数量/来源强度评 SUFFICIENT/PARTIAL/WEAK/NONE，驱动后续"够不够答"
 * </pre>
 * 算法精髓在两个内部类：
 * {@link RetrievalCandidate}（RRF 融合，{@code reciprocalRank} 即公式 1/(60+rank)）、
 * {@link RetrievalCluster}（聚簇与扩窗）。
 */
@Service
public class HybridChunkRetrievalService {

    private static final int DEFAULT_NEIGHBOR_WINDOW = 1;   // 邻居扩窗：簇前后各多取 1 片，补全上下文
    private static final int CHANNEL_TOP_K = 50;            // 每路召回上限：向量/关键词各取前 50 条
    private static final int RRF_K = 60;                    // RRF 公式常数：分 = 1/(K + rank)，K=60 为业界经验值

    private final PgVectorRetrievalAdapter vectorRetrievalAdapter;
    private final ElasticsearchChunkIndexService elasticsearchChunkIndexService;
    private final DocumentChunkMapper documentChunkMapper;
    private final QueryPlanningService queryPlanningService;
    private final int neighborWindow;

    @Autowired
    public HybridChunkRetrievalService(
            PgVectorRetrievalAdapter vectorRetrievalAdapter,
            ElasticsearchChunkIndexService elasticsearchChunkIndexService,
            DocumentChunkMapper documentChunkMapper,
            QueryPlanningService queryPlanningService
    ) {
        this(
                vectorRetrievalAdapter,
                elasticsearchChunkIndexService,
                documentChunkMapper,
                queryPlanningService,
                DEFAULT_NEIGHBOR_WINDOW
        );
    }

    public HybridChunkRetrievalService(
            PgVectorRetrievalAdapter vectorRetrievalAdapter,
            ElasticsearchChunkIndexService elasticsearchChunkIndexService,
            DocumentChunkMapper documentChunkMapper,
            QueryPlanningService queryPlanningService,
            int neighborWindow
    ) {
        this.vectorRetrievalAdapter = vectorRetrievalAdapter;
        this.elasticsearchChunkIndexService = elasticsearchChunkIndexService;
        this.documentChunkMapper = documentChunkMapper;
        this.queryPlanningService = queryPlanningService;
        this.neighborWindow = Math.max(0, neighborWindow);
    }

    public RetrievedEvidenceBundle retrieve(Long groupId, String question, int topK) {
        Long validGroupId = requirePositiveGroupId(groupId);
        String normalizedQuestion = requireQuestion(question);
        int validTopK = topK > 0 ? topK : 5;

        // ===== ① 查询规划：把一个问题改写成多个 query（直查/改写/拆分）=====
        // 目的：单一 query 召回不全，改写多个版本能撒更大的网、提升召回率。
        QueryPlanResult queryPlan = queryPlanningService.plan(normalizedQuestion);

        // candidates 以 chunkId 为 key 累积两路召回结果，同一 chunk 多次命中会被合并。
        Map<Long, RetrievalCandidate> candidates = new LinkedHashMap<>();

        // ===== ② 双通道召回：每个 query 都跑一遍 向量路 + 关键词路 =====
        // 向量路抓"语义相近"，关键词路抓"字面精确"，互补。每路各取前 CHANNEL_TOP_K=50。
        for (String plannedQuery : queryPlan.queries()) {
            mergeVectorHits(candidates, validGroupId, plannedQuery);
            mergeKeywordHits(candidates, validGroupId, plannedQuery);
        }

        if (candidates.isEmpty()) {
            return RetrievedEvidenceBundle.empty();
        }

        // ===== ③ RRF 融合后的最终排序：按累计 rankingScore 降序，取 topK =====
        // rankingScore 已经在 merge 阶段按 RRF 公式累加好（见 RetrievalCandidate）。
        List<RetrievalCandidate> rankedCandidates = candidates.values().stream()
                .sorted(Comparator
                        .comparingDouble(RetrievalCandidate::rankingScore).reversed()
                        .thenComparing(RetrievalCandidate::chunkId))
                .limit(validTopK)
                .toList();

        // ===== ④ 聚簇：同文档内 chunkIndex 相邻的命中合并成一个簇（一条证据）=====
        List<RetrievalCluster> rankedClusters = buildClusters(rankedCandidates);

        // 拉取这些 chunk 的元信息(文件名/正文等)，仅查一次、按 chunkId 索引。
        List<Long> chunkIds = rankedCandidates.stream().map(RetrievalCandidate::chunkId).toList();
        Map<Long, Map<String, Object>> rowByChunkId = indexRows(
                documentChunkMapper.selectQaReadyChunksByIds(validGroupId, chunkIds)
        );
        // 按 documentId 缓存"整篇的切片列表"，扩窗拼上下文时避免对同一文档重复查库。
        Map<Long, List<DocumentChunkEntity>> chunkWindowCache = new LinkedHashMap<>();

        // 把每个簇渲染成一条 Spring AI Document（含扩窗后的证据正文 + 元数据），编号 E1、E2…
        List<Document> documents = new ArrayList<>();
        int evidenceIndex = 1;
        for (RetrievalCluster cluster : rankedClusters) {
            Map<String, Object> row = rowByChunkId.get(cluster.primaryChunkId());
            if (row == null) {
                continue;
            }
            Document document = toDocument("E" + evidenceIndex, row, cluster, chunkWindowCache);
            if (document == null) {
                continue;
            }
            documents.add(document);
            evidenceIndex++;
        }
        if (documents.isEmpty()) {
            return RetrievedEvidenceBundle.empty();
        }

        // ===== ⑤ 证据分级：依据证据数量与来源强度评定级别，产出给模型的"回答指导语" =====
        // 级别会决定后续是"正常答""只答能答的部分"还是"直接拒答"（防幻觉核心）。
        EvidenceLevel evidenceLevel = evaluateEvidenceLevel(documents);
        return new RetrievedEvidenceBundle(documents, evidenceLevel, buildEvidenceGuidance(evidenceLevel));
    }

    private void mergeVectorHits(
            Map<Long, RetrievalCandidate> candidates,
            Long groupId,
            String query
    ) {
        // 向量路：pgvector 按"语义相近"召回（带 groupId 过滤 = 权限隔离）。
        List<PgVectorRetrievalAdapter.VectorHit> vectorHits = vectorRetrievalAdapter.search(groupId, query, CHANNEL_TOP_K);
        for (int index = 0; index < vectorHits.size(); index++) {
            PgVectorRetrievalAdapter.VectorHit hit = vectorHits.get(index);
            // 同一 chunk 可能被多个 query / 两路重复命中：computeIfAbsent 保证只建一个 candidate。
            RetrievalCandidate candidate = candidates.computeIfAbsent(
                    hit.chunkId(),
                    ignored -> RetrievalCandidate.fromVectorHit(hit)
            );
            // index+1 即该 chunk 在本路的"排名"，传给 candidate 累加 RRF 分数。
            candidate.mergeVectorHit(hit, index + 1);
        }
    }

    private void mergeKeywordHits(
            Map<Long, RetrievalCandidate> candidates,
            Long groupId,
            String query
    ) {
        // 关键词路：ES BM25 按"字面匹配"召回（同样带 groupId 过滤）。
        List<ElasticsearchChunkIndexService.KeywordHit> keywordHits =
                elasticsearchChunkIndexService.search(groupId, query, CHANNEL_TOP_K);
        for (int index = 0; index < keywordHits.size(); index++) {
            ElasticsearchChunkIndexService.KeywordHit hit = keywordHits.get(index);
            RetrievalCandidate candidate = candidates.computeIfAbsent(
                    hit.chunkId(),
                    ignored -> RetrievalCandidate.fromKeywordHit(hit)
            );
            candidate.mergeKeywordHit(hit, index + 1);
        }
    }

    private Map<Long, Map<String, Object>> indexRows(List<Map<String, Object>> rows) {
        Map<Long, Map<String, Object>> rowByChunkId = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            rowByChunkId.put(requireLong(getValue(row, "chunkId"), "chunkId"), row);
        }
        return rowByChunkId;
    }

    private Document toDocument(
            String evidenceId,
            Map<String, Object> row,
            RetrievalCluster cluster,
            Map<Long, List<DocumentChunkEntity>> chunkWindowCache
    ) {
        Long documentId = requireLong(getValue(row, "documentId"), "documentId");
        Integer chunkIndex = requireInteger(getValue(row, "chunkIndex"), "chunkIndex");
        if (!documentId.equals(cluster.documentId()) || !chunkIndex.equals(cluster.primaryChunkIndex())) {
            throw new BusinessException("检索结果与文档切片不一致");
        }
        Long chunkId = requireLong(getValue(row, "chunkId"), "chunkId");
        String fileName = requireText(getValue(row, "fileName"), "fileName");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("evidenceId", evidenceId);
        metadata.put("groupId", requireLong(getValue(row, "groupId"), "groupId"));
        metadata.put("documentId", documentId);
        metadata.put("chunkId", chunkId);
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("primaryChunkId", cluster.primaryChunkId());
        metadata.put("primaryChunkIndex", cluster.primaryChunkIndex());
        metadata.put("startChunkIndex", cluster.expandedStartChunkIndex(neighborWindow));
        metadata.put("endChunkIndex", cluster.expandedEndChunkIndex(neighborWindow));
        metadata.put("fileName", fileName);
        metadata.put("score", cluster.rankingScore());
        metadata.put("retrievalSource", cluster.source());
        metadata.put("vectorScore", cluster.vectorScore());
        metadata.put("keywordScore", cluster.keywordScore());
        metadata.put("hybridScore", cluster.rankingScore());
        String evidenceText = buildEvidenceWindow(row, cluster, chunkWindowCache);
        if (!StringUtils.hasText(evidenceText)) {
            return null;
        }
        return Document.builder()
                .id(evidenceId)
                .text(evidenceText)
                .metadata(metadata)
                .build();
    }

    private String buildEvidenceWindow(
            Map<String, Object> row,
            RetrievalCluster cluster,
            Map<Long, List<DocumentChunkEntity>> chunkWindowCache
    ) {
        Long groupId = requireLong(getValue(row, "groupId"), "groupId");
        Long documentId = requireLong(getValue(row, "documentId"), "documentId");
        String fileName = requireText(getValue(row, "fileName"), "fileName");
        List<DocumentChunkEntity> chunks = chunkWindowCache.computeIfAbsent(
                documentId,
                ignored -> documentChunkMapper.selectReadyActiveChunksByDocumentId(groupId, documentId)
        );
        if (chunks.isEmpty()) {
            return null;
        }
        int startIndex = cluster.expandedStartChunkIndex(neighborWindow);
        int endIndex = cluster.expandedEndChunkIndex(neighborWindow);
        StringBuilder builder = new StringBuilder();
        for (DocumentChunkEntity chunk : chunks) {
            if (chunk.getChunkIndex() != null
                    && chunk.getChunkIndex() >= startIndex
                    && chunk.getChunkIndex() <= endIndex
                    && StringUtils.hasText(chunk.getChunkText())) {
                if (!builder.isEmpty()) {
                    builder.append("\n");
                }
                builder.append(chunk.getChunkText().trim());
            }
        }
        if (builder.isEmpty()) {
            return null;
        }
        return "文件名：" + fileName + "\n" + builder;
    }

    private List<RetrievalCluster> buildClusters(List<RetrievalCandidate> rankedCandidates) {
        Map<Long, List<RetrievalCandidate>> candidatesByDocumentId = new LinkedHashMap<>();
        for (RetrievalCandidate candidate : rankedCandidates) {
            candidatesByDocumentId.computeIfAbsent(candidate.documentId(), ignored -> new ArrayList<>()).add(candidate);
        }
        List<RetrievalCluster> clusters = new ArrayList<>();
        for (List<RetrievalCandidate> sameDocumentCandidates : candidatesByDocumentId.values()) {
            List<RetrievalCandidate> sortedByChunkIndex = sameDocumentCandidates.stream()
                    .sorted(Comparator.comparing(RetrievalCandidate::chunkIndex))
                    .toList();
            RetrievalCluster currentCluster = null;
            for (RetrievalCandidate candidate : sortedByChunkIndex) {
                if (currentCluster == null || !currentCluster.isContinuousWith(candidate)) {
                    currentCluster = new RetrievalCluster(candidate);
                    clusters.add(currentCluster);
                    continue;
                }
                currentCluster.add(candidate);
            }
        }
        return clusters.stream()
                .sorted(Comparator
                        .comparingDouble(RetrievalCluster::rankingScore).reversed()
                        .thenComparing(RetrievalCluster::primaryChunkId))
                .toList();
    }

    /**
     * 证据分级——防幻觉的核心。在送进模型之前，先用确定性规则判断"证据够不够"，
     * 够才允许正常答，不够就限制或拒答。完全依赖规则（可解释、可审计、零额外模型调用）。
     *
     * <p>判定维度：
     * <ul>
     *   <li>证据数量（documents.size）</li>
     *   <li>来源强度：是否双通道共振(BOTH)——两路都命中通常意味着真相关</li>
     *   <li>最高向量分 topScore（≥0.95 表示高度相关，阈值偏严以减少幻觉）</li>
     * </ul>
     * 级别语义：SUFFICIENT 可正常答 / PARTIAL 只答能答的部分 / WEAK 谨慎答 / NONE 直接拒答。
     */
    private EvidenceLevel evaluateEvidenceLevel(List<Document> documents) {
        if (documents.isEmpty()) {
            return EvidenceLevel.NONE;
        }
        // 是否存在"双通道共振"的证据（向量+关键词两路都命中）。
        boolean hasBothSource = documents.stream()
                .map(document -> document.getMetadata().get("retrievalSource"))
                .anyMatch("BOTH"::equals);
        // 是否存在向量路命中的证据（VECTOR 或 BOTH）。
        boolean hasVectorEvidence = documents.stream()
                .map(document -> document.getMetadata().get("retrievalSource"))
                .anyMatch(source -> "VECTOR".equals(source) || "BOTH".equals(source));
        // 所有证据中最高的混合分，用于判断是否"高度相关"。
        double topScore = documents.stream()
                .map(document -> document.getMetadata().get("hybridScore"))
                .filter(Double.class::isInstance)
                .map(Double.class::cast)
                .max(Double::compareTo)
                .orElse(0D);
        // SUFFICIENT：证据≥2条 且（双通道共振 或 向量分极高）。最可信，允许正常作答。
        if (documents.size() >= 2 && (hasBothSource || (hasVectorEvidence && topScore >= 0.95D))) {
            return EvidenceLevel.SUFFICIENT;
        }
        // PARTIAL：有共振或有多条，但不够强。只答证据覆盖的部分，未覆盖要说明。
        if (hasBothSource || documents.size() >= 2) {
            return EvidenceLevel.PARTIAL;
        }
        // WEAK：只有一条弱证据，只能谨慎作答、明确依据有限。
        return EvidenceLevel.WEAK;
    }

    private String buildEvidenceGuidance(EvidenceLevel evidenceLevel) {
        return switch (evidenceLevel) {
            case NONE -> "当前没有可用证据，必须直接拒答。";
            case WEAK -> "当前证据相关性有限，只能谨慎回答，必须明确说明依据有限，不能给出确定性结论。";
            case PARTIAL -> "当前证据只覆盖部分问题，只能回答证据明确支持的部分，未覆盖部分必须明确说明不足。";
            case SUFFICIENT -> "当前证据较充分，可以正常回答，但仍然不得超出证据进行臆测。";
        };
    }

    private Object getValue(Map<String, Object> row, String field) {
        Object value = row.get(field);
        if (value != null) {
            return value;
        }
        return row.get(field.toLowerCase());
    }

    private Long requirePositiveGroupId(Long groupId) {
        if (groupId == null || groupId <= 0) {
            throw new BusinessException("groupId 非法");
        }
        return groupId;
    }

    private String requireQuestion(String question) {
        if (!StringUtils.hasText(question)) {
            throw new BusinessException("问题不能为空");
        }
        return question.trim();
    }

    private Long requireLong(Object value, String field) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new BusinessException("检索结果缺少字段: " + field);
    }

    private Integer requireInteger(Object value, String field) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new BusinessException("检索结果缺少字段: " + field);
    }

    private String requireText(Object value, String field) {
        if (value instanceof String text && StringUtils.hasText(text)) {
            return text.trim();
        }
        throw new BusinessException("检索结果缺少字段: " + field);
    }

    /**
     * 单个 chunk 的"候选"——累积它在向量路和关键词路的命中情况，并按 RRF 公式累加融合分。
     * RRF 的精髓：只看每路的"排名(rank)"，不看原始分数，从而避开两路打分尺度不一致的问题。
     */
    static final class RetrievalCandidate {

        private final Long documentId;
        private final Long chunkId;
        private final Integer chunkIndex;
        private double vectorScore;      // 向量路原始分（仅记录，不参与排序，避免尺度问题）
        private double keywordScore;     // 关键词路原始分（同上）
        private double rankingScore;     // RRF 累加分——真正用于排序的字段
        private boolean vectorMatched;   // 是否被向量路命中过
        private boolean keywordMatched;  // 是否被关键词路命中过

        private RetrievalCandidate(Long documentId, Long chunkId, Integer chunkIndex) {
            this.documentId = documentId;
            this.chunkId = chunkId;
            this.chunkIndex = chunkIndex;
        }

        static RetrievalCandidate fromVectorHit(PgVectorRetrievalAdapter.VectorHit hit) {
            return new RetrievalCandidate(hit.documentId(), hit.chunkId(), hit.chunkIndex());
        }

        static RetrievalCandidate fromKeywordHit(ElasticsearchChunkIndexService.KeywordHit hit) {
            return new RetrievalCandidate(hit.documentId(), hit.chunkId(), hit.chunkIndex());
        }

        /** 向量路命中：记录来源，累加 RRF 分。rank = 该 chunk 在向量路的排名(从1开始)。 */
        void mergeVectorHit(PgVectorRetrievalAdapter.VectorHit hit, int rank) {
            this.vectorMatched = true;
            this.vectorScore = Math.max(this.vectorScore, hit.score());
            this.rankingScore += reciprocalRank(rank);   // ← RRF：加分 = 1/(60+rank)
        }

        /** 关键词路命中：同上。两路都命中时 rankingScore 会有两项相加，天然占优。 */
        void mergeKeywordHit(ElasticsearchChunkIndexService.KeywordHit hit, int rank) {
            this.keywordMatched = true;
            this.keywordScore = Math.max(this.keywordScore, hit.normalizedScore());
            this.rankingScore += reciprocalRank(rank);   // ← RRF：加分 = 1/(60+rank)
        }

        Long documentId() {
            return documentId;
        }

        Long chunkId() {
            return chunkId;
        }

        Integer chunkIndex() {
            return chunkIndex;
        }

        double vectorScore() {
            return vectorScore;
        }

        double keywordScore() {
            return keywordScore;
        }

        double rankingScore() {
            return rankingScore;
        }

        /** 来源标记：两路都命中为 BOTH（强信号），否则标出具体哪一路。供证据分级使用。 */
        String source() {
            if (vectorMatched && keywordMatched) {
                return "BOTH";
            }
            return vectorMatched ? "VECTOR" : "KEYWORD";
        }

        /**
         * RRF 核心公式：返回某排名对应的加分 = 1/(K + rank)，K=60。
         * 名次越靠前(rank 越小)加分越多；+60 让尾部名次也保留体面的小分而非归零。
         * 用排名而非分数，是该算法能跨"尺度不可比"的两路做公平融合的关键。
         */
        private double reciprocalRank(int rank) {
            return 1D / (RRF_K + Math.max(rank, 1));
        }
    }

    /**
     * 证据"聚簇"——把同一文档内、chunkIndex 连续相邻的若干命中合并成一条证据。
     * 解决切片切碎语义的问题：一个完整论点横跨 3、4、5 片时，合并后不会被当成 3 条重复证据，
     * 也便于下一步"邻居扩窗"补全上下文。primary 取簇内 RRF 分最高者作为代表。
     */
    static final class RetrievalCluster {

        private final Long documentId;
        private final List<RetrievalCandidate> members = new ArrayList<>();
        private RetrievalCandidate primaryCandidate;   // 簇内得分最高的代表 chunk
        private int startChunkIndex;                   // 簇的起始切片序号
        private int endChunkIndex;                     // 簇的结束切片序号
        private double rankingScore;                   // 簇的 RRF 分（取成员最大值）
        private double vectorScore;
        private double keywordScore;
        private boolean hasVectorSource;
        private boolean hasKeywordSource;

        private RetrievalCluster(RetrievalCandidate seed) {
            this.documentId = seed.documentId();
            this.startChunkIndex = seed.chunkIndex();
            this.endChunkIndex = seed.chunkIndex();
            add(seed);
        }

        /** 连续性判定：同文档 且 候选片的序号正好紧接当前簇末尾(end+1)，才算同一簇。 */
        boolean isContinuousWith(RetrievalCandidate candidate) {
            return documentId.equals(candidate.documentId()) && candidate.chunkIndex() == endChunkIndex + 1;
        }

        /** 把一个候选并入簇：更新边界、累计最高分与来源，并维护得分最高的 primary 代表。 */
        void add(RetrievalCandidate candidate) {
            members.add(candidate);
            endChunkIndex = Math.max(endChunkIndex, candidate.chunkIndex());
            startChunkIndex = Math.min(startChunkIndex, candidate.chunkIndex());
            rankingScore = Math.max(rankingScore, candidate.rankingScore());
            vectorScore = Math.max(vectorScore, candidate.vectorScore());
            keywordScore = Math.max(keywordScore, candidate.keywordScore());
            hasVectorSource = hasVectorSource || "VECTOR".equals(candidate.source()) || "BOTH".equals(candidate.source());
            hasKeywordSource = hasKeywordSource || "KEYWORD".equals(candidate.source()) || "BOTH".equals(candidate.source());
            // primary：优先取 RRF 分高者；同分时取 chunkIndex 更小(更靠前)者，保证稳定排序。
            if (primaryCandidate == null
                    || candidate.rankingScore() > primaryCandidate.rankingScore()
                    || (candidate.rankingScore() == primaryCandidate.rankingScore()
                    && candidate.chunkIndex() < primaryCandidate.chunkIndex())) {
                primaryCandidate = candidate;
            }
        }

        Long documentId() {
            return documentId;
        }

        Long primaryChunkId() {
            return primaryCandidate.chunkId();
        }

        Integer primaryChunkIndex() {
            return primaryCandidate.chunkIndex();
        }

        double rankingScore() {
            return rankingScore;
        }

        double vectorScore() {
            return vectorScore;
        }

        double keywordScore() {
            return keywordScore;
        }

        /** 邻居扩窗后的起始序号：簇起点向前扩 neighborWindow 片(不越界到负数)。 */
        int expandedStartChunkIndex(int neighborWindow) {
            return Math.max(0, startChunkIndex - Math.max(0, neighborWindow));
        }

        /** 邻居扩窗后的结束序号：簇终点向后扩 neighborWindow 片。最终证据正文 = [start, end] 范围拼接。 */
        int expandedEndChunkIndex(int neighborWindow) {
            return endChunkIndex + Math.max(0, neighborWindow);
        }

        String source() {
            if (hasVectorSource && hasKeywordSource) {
                return "BOTH";
            }
            return hasVectorSource ? "VECTOR" : "KEYWORD";
        }
    }
}
