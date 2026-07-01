package com.dong.ddrag.retrieval.vectorstore;

import com.dong.ddrag.common.exception.BusinessException;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 向量检索适配器（语义检索那一路）。对应走读指南「链路 B」检索底层。
 *
 * <p>在 pgvector 上做"语义近邻搜索"——给一个问题文本，找意思最相近的 chunk。
 *
 * <p>⭐ 两个关键设计：
 * <ol>
 *   <li><b>带 groupId 过滤</b>：filterExpression 强制只在本组数据里搜——这是"权限隔离"在检索层的落点。</li>
 *   <li><b>双重校验</b>：返回结果还要再对一次 groupId（防御性，防止底层误返回跨组数据）。</li>
 * </ol>
 */
@Component
public class PgVectorRetrievalAdapter {

    private final VectorStore vectorStore;

    public PgVectorRetrievalAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<VectorHit> search(Long groupId, String question, int topK) {
        // ⭐ groupId 过滤：只在本组向量里做近邻搜索，实现多租户隔离。
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .filterExpression(new FilterExpressionBuilder().eq("groupId", groupId).build())
                .build();
        return vectorStore.similaritySearch(searchRequest).stream()
                .map(document -> toVectorHit(groupId, document))
                .toList();
    }

    private VectorHit toVectorHit(Long expectedGroupId, Document document) {
        Map<String, Object> metadata = document.getMetadata();
        Long groupId = requireLong(metadata, "groupId");
        // 防御性二次校验：即便底层过滤失效，也绝不让跨组数据漏出去。
        if (!expectedGroupId.equals(groupId)) {
            throw new BusinessException("向量检索返回了跨群组数据");
        }
        return new VectorHit(
                requireLong(metadata, "documentId"),
                requireLong(metadata, "chunkId"),
                requireInteger(metadata, "chunkIndex"),
                requireText(document.getText()),
                document.getScore() == null ? 0D : document.getScore()
        );
    }

    private Long requireLong(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException exception) {
                throw new BusinessException("向量检索元数据格式非法: " + key, exception);
            }
        }
        throw new BusinessException("向量检索缺少必要元数据: " + key);
    }

    private Integer requireInteger(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException exception) {
                throw new BusinessException("向量检索元数据格式非法: " + key, exception);
            }
        }
        throw new BusinessException("向量检索缺少必要元数据: " + key);
    }

    private String requireText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("向量检索返回空切片");
        }
        return text.trim();
    }

    public record VectorHit(
            Long documentId,
            Long chunkId,
            Integer chunkIndex,
            String chunkText,
            double score
    ) {
    }
}
