package com.dong.ddrag.assistant.agent;

import com.dong.ddrag.assistant.model.vo.tool.KnowledgeBaseSearchToolResponse;
import com.dong.ddrag.common.exception.BusinessException;
import com.dong.ddrag.qa.model.vo.AskQuestionResponse;
import com.dong.ddrag.qa.rag.ReadyChunkDocumentRetriever;
import com.dong.ddrag.qa.rag.RetrievedEvidenceBundle;
import com.dong.ddrag.qa.support.CitationAssembler;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * "知识库检索"被封装成一个 Agent 可调用的工具（Tool Calling）。
 * 对应走读指南「链路 C」。与 QA 链路不同：QA 是"每次必检索"，这里是"模型自己决定要不要检索"。
 *
 * <p>两个关键设计：
 * <ol>
 *   <li><b>groupId 通过 ToolContext 透传，而非写进 prompt</b>——防止用户用 prompt 注入越权访问别组数据。
 *       每次调用都从 context 取出当前合法的 groupId。</li>
 *   <li><b>防重复调用</b>：用 {@link AssistantKnowledgeBaseToolResultHolder} 记录本轮是否已检索过，
 *       若已检索则返回 DUPLICATE_TOOL_CALL，强制模型基于上次结果作答，避免反复调工具烧钱/死循环。</li>
 * </ol>
 * 底层检索复用 QA 链路的 {@link ReadyChunkDocumentRetriever}（同样的混合检索 + 证据分级）。
 */
@Component
public class AssistantKnowledgeBaseTool {

    public static final String TOOL_NAME = "knowledgeBaseSearch";
    public static final String GROUP_ID_CONTEXT_KEY = "groupId";
    public static final String RESULT_HOLDER_CONTEXT_KEY = "knowledgeBaseToolResultHolder";

    private static final String INSUFFICIENT_CODE = "INSUFFICIENT_EVIDENCE";
    private static final String INSUFFICIENT_MESSAGE = "检索到的有效证据不足，暂不回答。";

    private final ReadyChunkDocumentRetriever readyChunkDocumentRetriever;
    private final CitationAssembler citationAssembler;

    public AssistantKnowledgeBaseTool(
            ReadyChunkDocumentRetriever readyChunkDocumentRetriever,
            CitationAssembler citationAssembler
    ) {
        this.readyChunkDocumentRetriever = readyChunkDocumentRetriever;
        this.citationAssembler = citationAssembler;
    }

    @Tool(
            name = TOOL_NAME,
            description = "在当前已选择的知识库组内检索相关证据片段。只返回证据，不生成最终答案。"
    )
    public KnowledgeBaseSearchToolResponse search(
            @ToolParam(description = "用于检索知识库的查询文本，通常使用用户当前问题。") String query,
            ToolContext toolContext
    ) {
        // groupId 从 ToolContext 取（由上层按当前会话/权限注入），不信任 prompt 内容——防越权。
        Long groupId = readGroupId(toolContext);
        AssistantKnowledgeBaseToolResultHolder resultHolder = readResultHolder(toolContext);

        // 防重复调用守卫：本轮已检索过就不再检索，直接让模型用上一次的结果。
        if (resultHolder.hasCompletedSearch()) {
            return new KnowledgeBaseSearchToolResponse(
                    false,
                    "DUPLICATE_TOOL_CALL",
                    "本轮已经完成过一次知识库检索，请基于上一条工具返回的 evidences 直接给出最终回答，不要再次调用工具。",
                    null,
                    "本轮知识库检索结果已经返回，必须停止继续调用工具并生成最终回答。",
                    List.of(),
                    resultHolder.currentCitations()
            );
        }

        String safeQuery = requireQuery(query);
        // 复用 QA 链路的混合检索 + 证据分级（同一套底层能力）。
        RetrievedEvidenceBundle evidenceBundle = readyChunkDocumentRetriever.retrieveEvidence(groupId, safeQuery);
        List<Document> documents = evidenceBundle.documents();
        if (documents == null || documents.isEmpty()) {
            // 没召回到证据：记录空引用，返回"证据不足"让模型拒答（与 QA 链路一致的防幻觉策略）。
            resultHolder.recordCitations(List.of());
            return new KnowledgeBaseSearchToolResponse(
                    false,
                    INSUFFICIENT_CODE,
                    INSUFFICIENT_MESSAGE,
                    evidenceBundle.evidenceLevel() == null ? null : evidenceBundle.evidenceLevel().name(),
                    evidenceBundle.evidenceGuidance(),
                    List.of(),
                    List.of()
            );
        }
        // 组装引用清单并记录到 holder，供本轮后续/防重复判断使用。
        List<AskQuestionResponse.Citation> citations = citationAssembler.assembleDocuments(documents);
        resultHolder.recordCitations(citations);
        return new KnowledgeBaseSearchToolResponse(
                true,
                null,
                null,
                evidenceBundle.evidenceLevel() == null ? null : evidenceBundle.evidenceLevel().name(),
                evidenceBundle.evidenceGuidance(),
                documents.stream().map(this::toEvidence).toList(),
                citations
        );
    }

    private String requireQuery(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BusinessException("知识库检索 query 不能为空");
        }
        return query.trim();
    }

    private Long readGroupId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            throw new BusinessException("知识库检索缺少工具上下文");
        }
        Object value = toolContext.getContext().get(GROUP_ID_CONTEXT_KEY);
        if (value instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            try {
                long parsed = Long.parseLong(stringValue);
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        throw new BusinessException("知识库检索缺少 groupId");
    }

    private AssistantKnowledgeBaseToolResultHolder readResultHolder(ToolContext toolContext) {
        Object value = toolContext.getContext().get(RESULT_HOLDER_CONTEXT_KEY);
        if (value instanceof AssistantKnowledgeBaseToolResultHolder holder) {
            return holder;
        }
        throw new BusinessException("知识库检索缺少结果上下文");
    }

    private KnowledgeBaseSearchToolResponse.Evidence toEvidence(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new KnowledgeBaseSearchToolResponse.Evidence(
                readLong(metadata, "documentId"),
                readLong(metadata, "chunkId"),
                readInteger(metadata, "chunkIndex"),
                readText(metadata, "fileName"),
                readScore(metadata),
                document.getText()
        );
    }

    private Long readLong(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer readInteger(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value instanceof Number number ? number.intValue() : null;
    }

    private double readScore(Map<String, Object> metadata) {
        Object value = metadata.get("score");
        return value instanceof Number number ? number.doubleValue() : 0D;
    }

    private String readText(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value instanceof String text ? text : null;
    }
}
