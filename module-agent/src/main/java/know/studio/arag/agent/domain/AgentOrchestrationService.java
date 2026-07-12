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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrationService implements AgentApi {

    private static final int RETRIEVAL_TOP_K = 3;
    private static final int MAX_GROUNDING_EVIDENCE = 2;
    private static final int MAX_EVIDENCE_CHARS = 700;
    private static final Pattern ASCII_TERM_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]+");
    private static final Pattern NAMING_SUBJECT_PATTERN = Pattern.compile(
            "([\\p{IsHan}]{2,6})(?:如何|怎么|怎样)(?:进行)?命名"
    );
    private static final Set<String> QUESTION_STOP_TERMS = Set.of(
            "如何", "怎么", "怎样", "什么", "为何", "请问", "一下"
    );
    private static final String ANSWER_SYSTEM_PROMPT = """
            你是企业知识库问答助手。严格依据给定证据回答当前问题。
            先直接回答用户问的内容，问什么答什么；禁止改为概括整份文档或编造无关背景。
            只使用与问题直接相关的证据，忽略其他召回片段。证据不足时明确说明，不要编造。
            对“某类元素如何命名”这类问题，只能回答证据中主语明确是该元素的规则，不得混入方法、变量、接口或其他元素的规则。
            对简短事实问题，用一段话或最多三个要点回答，不要生成“总体概述”“总结”等无关章节。
            使用合法 Markdown：标题符号与标题文字之间必须有空格。
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
        Optional<String> extractiveAnswer = extractNamingRule(request.message(), bundle.evidence());
        if (extractiveAnswer.isPresent()) {
            return Flux.concat(
                    citationEvents(bundle.evidence()),
                    Flux.just(
                            ChatStreamEvent.token(extractiveAnswer.orElseThrow()),
                            new ChatStreamEvent(ChatStreamEvent.Type.DONE, donePayload(intent))
                    )
            );
        }
        return Flux.concat(
                citationEvents(bundle.evidence()),
                generate(
                        contextPrompt(context, request.message()),
                        evidencePrompt(request.message(), bundle.evidence()),
                        false,
                        intent
                )
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
                        evidencePrompt(request.message(), deduplicated),
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
        List<ConversationMessage> recentMessages = context.recentMessages();
        for (int index = 0; index < recentMessages.size(); index++) {
            ConversationMessage message = recentMessages.get(index);
            boolean duplicatedCurrentMessage = index == recentMessages.size() - 1
                    && message.role() == MessageRole.USER
                    && message.content().equals(currentMessage);
            if (duplicatedCurrentMessage) {
                continue;
            }
            prompt.append(message.role()).append(": ").append(message.content()).append('\n');
        }
        prompt.append("\n当前问题：").append(currentMessage);
        return prompt.toString();
    }

    private static String evidencePrompt(String question, List<Evidence> evidence) {
        StringBuilder prompt = new StringBuilder();
        List<Evidence> groundingEvidence = evidence.stream()
                .sorted(java.util.Comparator.comparingInt(
                        (Evidence item) -> evidenceRelevance(question, item.text())
                ).reversed())
                .limit(MAX_GROUNDING_EVIDENCE)
                .toList();
        int evidenceCount = groundingEvidence.size();
        for (int index = 0; index < evidenceCount; index++) {
            Evidence item = groundingEvidence.get(index);
            prompt.append("【证据 ").append(index + 1).append("｜")
                    .append(item.fileName()).append("#").append(item.chunkIndex()).append("】\n")
                    .append(focusEvidence(question, item.text())).append('\n');
        }
        return prompt.toString();
    }

    static String focusEvidence(String question, String text) {
        if (text.length() <= MAX_EVIDENCE_CHARS) {
            return text;
        }
        List<String> terms = focusTerms(question);
        if (terms.isEmpty()) {
            return text.substring(0, MAX_EVIDENCE_CHARS) + "…";
        }
        String normalizedText = text.toLowerCase(Locale.ROOT);
        String subject = namingSubject(question);
        int bestStart = 0;
        int bestScore = -1;
        for (String term : terms) {
            int occurrence = normalizedText.indexOf(term);
            while (occurrence >= 0) {
                int start = Math.max(0, Math.min(
                        occurrence - MAX_EVIDENCE_CHARS / 3,
                        text.length() - MAX_EVIDENCE_CHARS
                ));
                String window = normalizedText.substring(start, start + MAX_EVIDENCE_CHARS);
                int score = terms.stream().mapToInt(candidate ->
                        occurrenceCount(window, candidate) * candidate.length()
                ).sum() + subjectRuleScore(window, subject);
                if (score > bestScore) {
                    bestScore = score;
                    bestStart = start;
                }
                occurrence = normalizedText.indexOf(term, occurrence + term.length());
            }
        }
        int end = bestStart + MAX_EVIDENCE_CHARS;
        return (bestStart > 0 ? "…" : "")
                + text.substring(bestStart, end)
                + (end < text.length() ? "…" : "");
    }

    static int evidenceRelevance(String question, String text) {
        String normalizedText = text.toLowerCase(Locale.ROOT);
        int termScore = focusTerms(question).stream()
                .mapToInt(term -> occurrenceCount(normalizedText, term) * term.length())
                .sum();
        return termScore + subjectRuleScore(normalizedText, namingSubject(question));
    }

    static Optional<String> extractNamingRule(String question, List<Evidence> evidence) {
        String subject = namingSubject(question);
        if (subject.isBlank()) {
            return Optional.empty();
        }
        Pattern explicitRule = Pattern.compile(
                ".*【[^】]+】.*" + Pattern.quote(subject)
                        + ".{0,20}(?:使用|采用|命名|风格|应当|应该|必须|开头|结尾).*"
        );
        List<Evidence> ranked = evidence.stream()
                .sorted(java.util.Comparator.comparingInt(
                        (Evidence item) -> evidenceRelevance(question, item.text())
                ).reversed())
                .toList();
        for (Evidence item : ranked) {
            List<String> lines = item.text().lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .toList();
            for (int index = 0; index < lines.size(); index++) {
                if (!explicitRule.matcher(lines.get(index)).matches()) {
                    continue;
                }
                LinkedHashSet<String> selected = new LinkedHashSet<>();
                selected.add(lines.get(index));
                for (int next = index + 1; next < lines.size() && selected.size() < 4; next++) {
                    String line = lines.get(next);
                    if (line.matches("^\\d+\\.\\s*【.*")) {
                        break;
                    }
                    if (line.startsWith("正例：")
                            || line.startsWith("反例：")
                            || selected.size() == 1) {
                        selected.add(line);
                    }
                }
                return Optional.of("根据知识库规范：\n\n" + String.join("\n\n", selected));
            }
        }
        return Optional.empty();
    }

    private static List<String> focusTerms(String question) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        Matcher matcher = ASCII_TERM_PATTERN.matcher(question);
        while (matcher.find()) {
            terms.add(matcher.group().toLowerCase(Locale.ROOT));
        }
        String han = question.replaceAll("[^\\p{IsHan}]", "");
        for (int index = 0; index + 2 <= han.length(); index++) {
            String term = han.substring(index, index + 2);
            if (!QUESTION_STOP_TERMS.contains(term)) {
                terms.add(term);
            }
        }
        return List.copyOf(terms);
    }

    private static int occurrenceCount(String text, String term) {
        int count = 0;
        int index = text.indexOf(term);
        while (index >= 0) {
            count++;
            index = text.indexOf(term, index + term.length());
        }
        return count;
    }

    private static String namingSubject(String question) {
        Matcher matcher = NAMING_SUBJECT_PATTERN.matcher(question.replaceAll("\\s+", ""));
        if (!matcher.find()) {
            return "";
        }
        String subject = matcher.group(1);
        return subject.length() <= 4 ? subject : subject.substring(subject.length() - 4);
    }

    private static int subjectRuleScore(String text, String subject) {
        if (subject.isBlank()) {
            return 0;
        }
        Pattern rulePattern = Pattern.compile(
                Pattern.quote(subject) + ".{0,16}(?:使用|采用|命名|风格|应当|应该|必须|开头|结尾)",
                Pattern.DOTALL
        );
        int score = occurrenceCount(text, subject) * 8;
        Matcher matcher = rulePattern.matcher(text);
        while (matcher.find()) {
            score += 100;
        }
        return score;
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
