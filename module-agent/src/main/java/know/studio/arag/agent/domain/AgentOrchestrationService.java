package know.studio.arag.agent.domain;

import know.studio.arag.agent.api.AgentApi;
import know.studio.arag.agent.api.ChatRequest;
import know.studio.arag.agent.api.ChatStreamEvent;
import know.studio.arag.agent.api.IntentResult;
import know.studio.arag.conversation.api.AppendMessageCommand;
import know.studio.arag.conversation.api.ConversationApi;
import know.studio.arag.conversation.api.ConversationContext;
import know.studio.arag.conversation.api.ConversationMessage;
import know.studio.arag.conversation.api.MessageRole;
import know.studio.arag.identity.api.CurrentIdentity;
import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.platform.ai.chat.ChatModelRouter;
import know.studio.arag.platform.ai.provider.ChatChunk;
import know.studio.arag.platform.core.trace.RagTraceNode;
import know.studio.arag.retrieval.api.Evidence;
import know.studio.arag.retrieval.api.EvidenceBundle;
import know.studio.arag.retrieval.api.EvidenceLevel;
import know.studio.arag.retrieval.api.RetrievalApi;
import know.studio.arag.retrieval.api.RetrievalQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrationService implements AgentApi {

    private static final int RETRIEVAL_TOP_K = 5;
    private static final String ANSWER_SYSTEM_PROMPT = """
            你是 Agentic RAG 助手。严格依据给定上下文、证据或工具结果回答。
            不确定时明确说明，不要编造。回答简洁，并在使用证据时标注文件名。
            """;

    private final IdentityApi identityApi;
    private final ConversationApi conversationApi;
    private final RetrievalApi retrievalApi;
    private final IntentClassifier intentClassifier;
    private final AgentToolRegistry toolRegistry;
    private final QuestionDecomposer questionDecomposer;
    private final ChatModelRouter chatModelRouter;

    @Override
    @RagTraceNode("agent.orchestration")
    public Flux<ChatStreamEvent> streamChat(ChatRequest request) {
        CurrentIdentity identity = identityApi.currentUser();
        conversationApi.appendMessageForOwner(new AppendMessageCommand(
                request.sessionId(),
                MessageRole.USER,
                request.message(),
                estimateTokens(request.message()),
                Map.of()
        ), identity.userId());
        ConversationContext context = conversationApi.loadContextForOwner(
                identity.userId(),
                request.sessionId(),
                ""
        );
        IntentResult intent = intentClassifier.classify(request.message(), request.toolMode());
        Flux<ChatStreamEvent> response = Flux.defer(() -> route(request, context, intent, identity.userId()));
        return persistAssistant(response, request, identity.userId());
    }

    private Flux<ChatStreamEvent> route(
            ChatRequest request,
            ConversationContext context,
            IntentResult intent,
            long ownerUserId
    ) {
        if (intent.intent() == know.studio.arag.agent.api.IntentType.CLARIFY) {
            String clarification = intent.clarification().isBlank()
                    ? "请补充更具体的问题或查询对象。"
                    : intent.clarification();
            return Flux.just(
                    ChatStreamEvent.token(clarification),
                    new ChatStreamEvent(ChatStreamEvent.Type.DONE, donePayload(intent))
            );
        }
        if (request.deepThinking()) {
            return deepThinking(request, context, intent);
        }
        return switch (intent.intent()) {
            case KNOWLEDGE -> knowledgeAnswer(request, context, intent);
            case TOOL -> toolAnswer(request, context, intent, ownerUserId);
            case CHAT -> generate(contextPrompt(context, request.message()), "", false, intent);
            case CLARIFY -> Flux.empty();
        };
    }

    private Flux<ChatStreamEvent> knowledgeAnswer(
            ChatRequest request,
            ConversationContext context,
            IntentResult intent
    ) {
        EvidenceBundle bundle = retrievalApi.retrieve(new RetrievalQuery(
                request.message(),
                request.knowledgeBaseIds(),
                RETRIEVAL_TOP_K
        ));
        if (bundle.level() == EvidenceLevel.NONE) {
            return Flux.just(
                    ChatStreamEvent.token(bundle.guidance()),
                    new ChatStreamEvent(ChatStreamEvent.Type.DONE, donePayload(intent))
            );
        }
        return Flux.concat(
                citationEvents(bundle.evidence()),
                generate(contextPrompt(context, request.message()), evidencePrompt(bundle.evidence()), false, intent)
        );
    }

    private Flux<ChatStreamEvent> toolAnswer(
            ChatRequest request,
            ConversationContext context,
            IntentResult intent,
            long ownerUserId
    ) {
        AgentTool tool = toolRegistry.find(request.message()).orElse(null);
        if (tool == null) {
            log.info(
                    "No matching tool, falling back to knowledge retrieval sessionId={}",
                    request.sessionId()
            );
            return knowledgeAnswer(request, context, intent);
        }
        ResultHolder holder = new ResultHolder();
        String key = tool.name() + ':' + ownerUserId + ':' + request.message();
        ToolResult result = holder.getOrCompute(key, () -> tool.execute(ownerUserId, request.message()));
        return Flux.concat(
                Flux.just(
                        new ChatStreamEvent(ChatStreamEvent.Type.TOOL_CALL, Map.of("name", tool.name())),
                        new ChatStreamEvent(ChatStreamEvent.Type.TOOL_RESULT, result)
                ),
                generate(contextPrompt(context, request.message()), result.content(), false, intent)
        );
    }

    private Flux<ChatStreamEvent> deepThinking(
            ChatRequest request,
            ConversationContext context,
            IntentResult intent
    ) {
        List<String> subQuestions = questionDecomposer.decompose(request.message());
        List<Evidence> evidence = new ArrayList<>();
        List<ChatStreamEvent> thinking = new ArrayList<>();
        for (int index = 0; index < subQuestions.size(); index++) {
            String subQuestion = subQuestions.get(index);
            thinking.add(ChatStreamEvent.thinking(
                    "步骤 " + (index + 1) + "/" + subQuestions.size() + "：" + subQuestion
            ));
            EvidenceBundle bundle = retrievalApi.retrieve(new RetrievalQuery(
                    subQuestion,
                    request.knowledgeBaseIds(),
                    RETRIEVAL_TOP_K
            ));
            evidence.addAll(bundle.evidence());
        }
        List<Evidence> deduplicated = deduplicateEvidence(evidence);
        return Flux.concat(
                Flux.fromIterable(thinking),
                citationEvents(deduplicated),
                generate(
                        contextPrompt(context, request.message()),
                        evidencePrompt(deduplicated),
                        true,
                        intent
                )
        );
    }

    private Flux<ChatStreamEvent> generate(
            String context,
            String grounding,
            boolean reasoning,
            IntentResult intent
    ) {
        String prompt = context + (grounding.isBlank() ? "" : "\n\n可用资料：\n" + grounding);
        know.studio.arag.platform.ai.provider.ChatRequest aiRequest =
                new know.studio.arag.platform.ai.provider.ChatRequest(
                        ANSWER_SYSTEM_PROMPT,
                        prompt,
                        reasoning,
                        Map.of()
                );
        Flux<ChatStreamEvent> chunks = chatModelRouter.stream(aiRequest)
                .map(this::toStreamEvent);
        return chunks.concatWithValues(new ChatStreamEvent(ChatStreamEvent.Type.DONE, donePayload(intent)));
    }

    private Flux<ChatStreamEvent> persistAssistant(
            Flux<ChatStreamEvent> response,
            ChatRequest request,
            long ownerUserId
    ) {
        StringBuilder answer = new StringBuilder();
        List<Map<String, Object>> citations = new ArrayList<>();
        return response
                .doOnNext(event -> {
                    if (event.type() == ChatStreamEvent.Type.TOKEN) {
                        answer.append(event.payload());
                    } else if (event.type() == ChatStreamEvent.Type.CITATION
                            && event.payload() instanceof Map<?, ?> payload) {
                        citations.add(copyCitation(payload));
                    }
                })
                .doOnComplete(() -> {
                    if (!answer.isEmpty()) {
                        conversationApi.appendMessageForOwner(new AppendMessageCommand(
                                request.sessionId(),
                                MessageRole.ASSISTANT,
                                answer.toString(),
                                estimateTokens(answer.toString()),
                                assistantMetadata(citations)
                        ), ownerUserId);
                    }
                })
                .onErrorResume(exception -> {
                    log.error(
                            "Agent stream failed sessionId={}",
                            request.sessionId(),
                            exception
                    );
                    return Flux.just(new ChatStreamEvent(
                            ChatStreamEvent.Type.ERROR,
                            Map.of("message", "Agent 执行失败，请稍后重试")
                    ));
                });
    }

    private ChatStreamEvent toStreamEvent(ChatChunk chunk) {
        return chunk.type() == ChatChunk.Type.THINKING
                ? ChatStreamEvent.thinking(chunk.content())
                : ChatStreamEvent.token(chunk.content());
    }

    private static Flux<ChatStreamEvent> citationEvents(List<Evidence> evidence) {
        return Flux.fromIterable(evidence)
                .map(item -> new ChatStreamEvent(ChatStreamEvent.Type.CITATION, Map.of(
                        "knowledgeBaseId", Long.toString(item.knowledgeBaseId()),
                        "documentId", Long.toString(item.documentId()),
                        "chunkId", Long.toString(item.chunkId()),
                        "chunkIndex", item.chunkIndex(),
                        "fileName", item.fileName(),
                        "score", item.score(),
                        "snippet", item.text()
                )));
    }

    private static Map<String, Object> donePayload(IntentResult intent) {
        return Map.of("intent", intent.intent().name(), "confidence", intent.confidence());
    }

    private static String contextPrompt(ConversationContext context, String currentMessage) {
        StringBuilder prompt = new StringBuilder();
        if (!context.compactSummary().isBlank()) {
            prompt.append("会话摘要：").append(context.compactSummary()).append("\n\n");
        }
        prompt.append("最近消息：\n");
        for (ConversationMessage message : context.recentMessages()) {
            prompt.append(message.role()).append(": ").append(message.content()).append('\n');
        }
        prompt.append("\n当前问题：").append(currentMessage);
        return prompt.toString();
    }

    private static String evidencePrompt(List<Evidence> evidence) {
        StringBuilder prompt = new StringBuilder();
        for (Evidence item : evidence) {
            prompt.append('[').append(item.fileName()).append("#").append(item.chunkIndex()).append("] ")
                    .append(item.text()).append('\n');
        }
        return prompt.toString();
    }

    private static List<Evidence> deduplicateEvidence(List<Evidence> evidence) {
        Map<Long, Evidence> unique = new LinkedHashMap<>();
        evidence.forEach(item -> unique.putIfAbsent(item.chunkId(), item));
        return unique.values().stream().limit(10).toList();
    }

    private static Map<String, Object> copyCitation(Map<?, ?> payload) {
        Map<String, Object> citation = new LinkedHashMap<>();
        payload.forEach((key, value) -> citation.put(String.valueOf(key), value));
        return Map.copyOf(citation);
    }

    private static Map<String, Object> assistantMetadata(List<Map<String, Object>> citations) {
        if (citations.isEmpty()) {
            return Map.of();
        }
        List<String> knowledgeBaseIds = citations.stream()
                .map(citation -> String.valueOf(citation.get("knowledgeBaseId")))
                .distinct()
                .toList();
        return Map.of(
                "knowledgeBaseIds", knowledgeBaseIds,
                "citations", List.copyOf(citations)
        );
    }

    private static int estimateTokens(String content) {
        return Math.max(1, (content.length() + 3) / 4);
    }
}
