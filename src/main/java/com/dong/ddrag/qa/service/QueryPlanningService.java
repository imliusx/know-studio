package com.dong.ddrag.qa.service;

import com.dong.ddrag.common.exception.BusinessException;
import com.dong.ddrag.qa.model.QueryPlanResult;
import com.dong.ddrag.qa.model.QueryPlanStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 查询规划——RAG 检索的第①步（对应走读指南「链路 B」）。
 * 把用户一个问题交给大模型改写成多个检索 query，"撒更大的网"提升召回率。
 *
 * <p>三种策略（{@link QueryPlanStrategy}）：
 * <ul>
 *   <li>DIRECT：直接用原问题（问题本身已够明确）</li>
 *   <li>REWRITE：换种说法，原问题 + 改写版一起检索</li>
 *   <li>DECOMPOSE：拆成多个子问题分别检索</li>
 * </ul>
 * 最终最多产出 {@value MAX_QUERY_COUNT} 个 query，供 HybridChunkRetrievalService 逐个跑双通道。
 * 规划失败时降级为 DIRECT（用原问题），保证主链路不被这步拖垮。
 */
@Service
public class QueryPlanningService {

    private static final Logger log = LoggerFactory.getLogger(QueryPlanningService.class);
    private static final int MAX_QUERY_COUNT = 3;   // 单次规划最多产出 3 个 query，控成本/控延迟

    private final ChatClient queryPlanningChatClient;
    private final PromptTemplate queryPlanningUserPromptTemplate;

    public QueryPlanningService(
            @Qualifier("queryPlanningChatClient") ChatClient queryPlanningChatClient,
            @Qualifier("queryPlanningUserPromptTemplate") PromptTemplate queryPlanningUserPromptTemplate
    ) {
        this.queryPlanningChatClient = queryPlanningChatClient;
        this.queryPlanningUserPromptTemplate = queryPlanningUserPromptTemplate;
    }

    public QueryPlanResult plan(String question) {
        String normalizedQuestion = requireQuestion(question);
        try {

            Prompt planPrompt = queryPlanningUserPromptTemplate.create(Map.of("question", normalizedQuestion));

            QueryPlanResult rawResult = queryPlanningChatClient.prompt(planPrompt)
//                    .user(user -> user.text(renderUserPrompt(normalizedQuestion)))
                    .call()
                    .entity(QueryPlanResult.class);
            return validatePlan(rawResult, normalizedQuestion);
        } catch (RuntimeException exception) {
            log.warn("Query planning failed, fallback to direct query. question={}", normalizedQuestion, exception);
            return QueryPlanResult.fallback(normalizedQuestion);
        }
    }

    private QueryPlanResult validatePlan(QueryPlanResult rawResult, String originalQuestion) {
        if (rawResult == null || rawResult.strategy() == null) {
            return QueryPlanResult.fallback(originalQuestion);
        }
        Set<String> normalizedQueries = normalizeQueries(rawResult.queries());
        if (normalizedQueries.isEmpty()) {
            return QueryPlanResult.fallback(originalQuestion);
        }
        List<String> finalQueries = switch (rawResult.strategy()) {
            case DIRECT -> List.of(originalQuestion);
            case REWRITE -> buildRewriteQueries(originalQuestion, normalizedQueries);
            case DECOMPOSE -> limitQueries(normalizedQueries);
        };
        if (finalQueries.isEmpty()) {
            return QueryPlanResult.fallback(originalQuestion);
        }
        return new QueryPlanResult(rawResult.strategy(), finalQueries);
    }

    private List<String> buildRewriteQueries(String originalQuestion, Set<String> normalizedQueries) {
        LinkedHashSet<String> rewriteQueries = new LinkedHashSet<>();
        rewriteQueries.add(originalQuestion);
        rewriteQueries.addAll(normalizedQueries);
        return limitQueries(rewriteQueries);
    }

    private Set<String> normalizeQueries(List<String> queries) {
        LinkedHashSet<String> normalizedQueries = new LinkedHashSet<>();
        if (queries == null) {
            return normalizedQueries;
        }
        for (String query : queries) {
            if (!StringUtils.hasText(query)) {
                continue;
            }
            String normalized = query.replaceAll("\\s+", " ").trim();
            if (StringUtils.hasText(normalized)) {
                normalizedQueries.add(normalized);
            }
        }
        return normalizedQueries;
    }

    private List<String> limitQueries(Set<String> queries) {
        return queries.stream()
                .limit(MAX_QUERY_COUNT)
                .toList();
    }

    private String renderUserPrompt(String question) {
        return queryPlanningUserPromptTemplate.render(Map.of("question", question));
    }

    private String requireQuestion(String question) {
        if (!StringUtils.hasText(question)) {
            throw new BusinessException("问题不能为空");
        }
        return question.replaceAll("\\s+", " ").trim();
    }
}
