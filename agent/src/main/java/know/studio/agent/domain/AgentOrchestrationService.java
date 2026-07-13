package know.studio.agent.domain;

import know.studio.agent.api.AgentApi;
import know.studio.agent.api.ChatRequest;
import know.studio.agent.api.ChatStreamEvent;
import know.studio.agent.api.IntentResult;
import know.studio.agent.prompt.AgentPromptCatalog;
import know.studio.chat.api.AppendMessageCommand;
import know.studio.chat.api.ConversationApi;
import know.studio.chat.api.ConversationContext;
import know.studio.chat.api.ConversationMessage;
import know.studio.chat.api.MessageRole;
import know.studio.auth.api.CurrentIdentity;
import know.studio.auth.api.IdentityApi;
import know.studio.ai.chat.ChatModelRouter;
import know.studio.ai.provider.ChatChunk;
import know.studio.ai.provider.ChatMessage;
import know.studio.ai.provider.GenerationProfile;
import know.studio.ai.prompt.PromptResource;
import know.studio.common.trace.RagTraceNode;
import know.studio.search.api.Evidence;
import know.studio.search.api.EvidenceBundle;
import know.studio.search.api.EvidenceLevel;
import know.studio.search.api.RetrievalApi;
import know.studio.search.api.RetrievalQuery;
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
    private static final int NAMING_RETRIEVAL_TOP_K = 20;
    private static final int MAX_GROUNDING_EVIDENCE = 2;
    private static final int MAX_EVIDENCE_CHARS = 700;
    private static final Pattern ASCII_TERM_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]+");
    private static final Pattern NAMING_SUBJECT_PATTERN = Pattern.compile(
            "([\\p{IsHan}]{2,6})(?:如何|怎么|怎样)(?:进行)?命名"
    );
    private static final List<String> NAMING_SUBJECT_SUFFIXES = List.of(
            "成员变量", "局部变量", "唯一索引", "普通索引", "组合索引", "主键索引",
            "异常类", "测试类", "抽象类", "实现类", "接口类",
            "方法名", "参数名", "字段名", "变量名", "常量名",
            "类名", "包名", "表名", "索引", "常量", "变量", "方法", "参数", "字段", "接口"
    );
    private static final Set<String> QUESTION_STOP_TERMS = Set.of(
            "如何", "怎么", "怎样", "什么", "为何", "请问", "一下"
    );
    private final IdentityApi identityApi;
    private final ConversationApi conversationApi;
    private final RetrievalApi retrievalApi;
    private final IntentClassifier intentClassifier;
    private final AgentToolRegistry toolRegistry;
    private final QuestionDecomposer questionDecomposer;
    private final ChatModelRouter chatModelRouter;
    private final AgentPromptCatalog promptCatalog;

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
                request.message()
        );
        String contextualQuestion = contextualQuestion(context, request.message());
        ChatRequest routedRequest = contextualQuestion.equals(request.message())
                ? request
                : new ChatRequest(
                        request.sessionId(),
                        contextualQuestion,
                        request.knowledgeBaseIds(),
                        request.toolMode(),
                        request.deepThinking()
                );
        IntentResult intent = intentClassifier.classify(routedRequest.message(), routedRequest.toolMode());
        Flux<ChatStreamEvent> response = Flux.defer(() -> route(
                routedRequest,
                context,
                intent,
                identity.userId()
        ));
        return persistAssistant(response, request, identity.userId());
    }

    private Flux<ChatStreamEvent> route(
            ChatRequest request,
            ConversationContext context,
            IntentResult intent,
            long ownerUserId
    ) {
        if (intent.intent() == know.studio.agent.api.IntentType.CLARIFY) {
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
            case CHAT -> generate(
                    context,
                    request.message(),
                    promptCatalog.chat(),
                    request.message(),
                    GenerationProfile.CHAT,
                    false,
                    intent
            );
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
                retrievalTopK(request.message())
        ));
        if (bundle.level() == EvidenceLevel.NONE) {
            return Flux.just(
                    ChatStreamEvent.token(bundle.guidance()),
                    new ChatStreamEvent(ChatStreamEvent.Type.DONE, donePayload(intent))
            );
        }
        List<Evidence> groundingEvidence = groundingEvidence(request.message(), bundle.evidence());
        return Flux.concat(
                citationEvents(groundingEvidence),
                generate(
                        context,
                        request.message(),
                        promptCatalog.knowledge(),
                        promptCatalog.knowledgeUser(
                                request.message(),
                                bundle.level().name(),
                                bundle.guidance(),
                                evidencePrompt(request.message(), groundingEvidence)
                        ),
                        GenerationProfile.KNOWLEDGE,
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
                generate(
                        context,
                        request.message(),
                        promptCatalog.knowledge(),
                        promptCatalog.knowledgeUser(
                                request.message(),
                                EvidenceLevel.SUFFICIENT.name(),
                                "业务工具已返回可用结果，只回答工具结果明确支持的内容。",
                                result.content()
                        ),
                        GenerationProfile.KNOWLEDGE,
                        false,
                        intent
                )
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
                    retrievalTopK(subQuestion)
            ));
            evidence.addAll(bundle.evidence());
        }
        List<Evidence> deduplicated = deduplicateEvidence(evidence);
        List<Evidence> groundingEvidence = groundingEvidence(request.message(), deduplicated);
        return Flux.concat(
                Flux.fromIterable(thinking),
                citationEvents(groundingEvidence),
                generate(
                        context,
                        request.message(),
                        promptCatalog.knowledge(),
                        promptCatalog.knowledgeUser(
                                request.message(),
                                EvidenceLevel.SUFFICIENT.name(),
                                "综合多个子问题的证据回答，不得超出证据范围。",
                                evidencePrompt(request.message(), groundingEvidence)
                        ),
                        GenerationProfile.KNOWLEDGE,
                        true,
                        intent
                )
        );
    }

    private Flux<ChatStreamEvent> generate(
            ConversationContext context,
            String currentMessage,
            PromptResource systemPrompt,
            String userPrompt,
            GenerationProfile profile,
            boolean reasoning,
            IntentResult intent
    ) {
        know.studio.ai.provider.ChatRequest aiRequest =
                new know.studio.ai.provider.ChatRequest(
                        systemPrompt.text(),
                        historyMessages(context, currentMessage),
                        userPrompt,
                        reasoning,
                        profile,
                        systemPrompt.version(),
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

    static List<ChatMessage> historyMessages(ConversationContext context, String currentMessage) {
        List<ChatMessage> history = new ArrayList<>();
        if (!context.compactSummary().isBlank()) {
            history.add(ChatMessage.system("会话摘要：\n" + context.compactSummary()));
        }
        if (!context.sessionSummary().isBlank()
                && !context.sessionSummary().equals(context.compactSummary())) {
            history.add(ChatMessage.system("会话记忆：\n" + context.sessionSummary()));
        }
        List<ConversationMessage> recentMessages = context.recentMessages();
        String originalCurrentMessage = context.currentQuestion().isBlank()
                ? currentMessage
                : context.currentQuestion();
        for (int index = 0; index < recentMessages.size(); index++) {
            ConversationMessage message = recentMessages.get(index);
            boolean duplicatedCurrentMessage = index == recentMessages.size() - 1
                    && message.role() == MessageRole.USER
                    && message.content().equals(originalCurrentMessage);
            if (duplicatedCurrentMessage) {
                continue;
            }
            switch (message.role()) {
                case USER -> history.add(ChatMessage.user(message.content()));
                case ASSISTANT -> history.add(ChatMessage.assistant(message.content()));
                case TOOL -> history.add(ChatMessage.assistant("工具结果：\n" + message.content()));
            }
        }
        return List.copyOf(history);
    }

    private static String evidencePrompt(String question, List<Evidence> evidence) {
        StringBuilder prompt = new StringBuilder();
        int evidenceCount = evidence.size();
        for (int index = 0; index < evidenceCount; index++) {
            Evidence item = evidence.get(index);
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

    static List<Evidence> groundingEvidence(String question, List<Evidence> evidence) {
        List<Evidence> ranked = evidence.stream()
                .sorted(java.util.Comparator
                        .comparingInt((Evidence item) -> namingRulePriority(question, item.text()))
                        .thenComparingDouble(item -> questionTermCoverage(question, item.text()))
                        .thenComparingInt(item -> evidenceRelevance(question, item.text()))
                        .reversed())
                .toList();
        if (ranked.isEmpty()) {
            return List.of();
        }
        if (hasExplicitNamingRule(question, ranked.getFirst().text())) {
            return List.of(ranked.getFirst());
        }
        double bestCoverage = questionTermCoverage(question, ranked.getFirst().text());
        if (bestCoverage >= 0.45) {
            return List.of(ranked.getFirst());
        }
        double minimumCoverage = Math.max(0.2, bestCoverage * 0.6);
        List<Evidence> selected = new ArrayList<>();
        selected.add(ranked.getFirst());
        ranked.stream()
                .skip(1)
                .filter(item -> questionTermCoverage(question, item.text()) >= minimumCoverage)
                .limit(MAX_GROUNDING_EVIDENCE - 1L)
                .forEach(selected::add);
        return List.copyOf(selected);
    }

    private static double questionTermCoverage(String question, String text) {
        List<String> terms = focusTerms(question);
        if (terms.isEmpty()) {
            return 0.0;
        }
        String normalizedText = text.toLowerCase(Locale.ROOT);
        long matched = terms.stream().filter(normalizedText::contains).count();
        return (double) matched / terms.size();
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

    static String namingSubject(String question) {
        String normalizedQuestion = question.replaceAll("\\s+", "");
        Matcher matcher = NAMING_SUBJECT_PATTERN.matcher(normalizedQuestion);
        if (!matcher.find()) {
            if (normalizedQuestion.contains("命名")) {
                for (String suffix : NAMING_SUBJECT_SUFFIXES) {
                    if (normalizedQuestion.contains(suffix)) {
                        return suffix;
                    }
                }
            }
            return "";
        }
        String subject = matcher.group(1);
        int possessiveIndex = subject.lastIndexOf('的');
        if (possessiveIndex >= 0 && possessiveIndex < subject.length() - 1) {
            subject = subject.substring(possessiveIndex + 1);
        }
        for (String suffix : NAMING_SUBJECT_SUFFIXES) {
            if (subject.endsWith(suffix)) {
                return suffix;
            }
        }
        return subject.length() <= 4 ? subject : subject.substring(subject.length() - 4);
    }

    static String contextualQuestion(ConversationContext context, String currentMessage) {
        String followUpSubject = namingFollowUpSubject(currentMessage);
        if (followUpSubject.isBlank()) {
            return currentMessage;
        }
        List<ConversationMessage> messages = context.recentMessages();
        String originalCurrentMessage = context.currentQuestion().isBlank()
                ? currentMessage
                : context.currentQuestion();
        for (int index = messages.size() - 1; index >= 0; index--) {
            ConversationMessage message = messages.get(index);
            if (index == messages.size() - 1
                    && message.role() == MessageRole.USER
                    && message.content().equals(originalCurrentMessage)) {
                continue;
            }
            if (message.role() != MessageRole.USER) {
                continue;
            }
            return replaceNamingSubject(message.content(), followUpSubject)
                    .orElse(currentMessage);
        }
        return currentMessage;
    }

    private static String namingFollowUpSubject(String question) {
        String normalized = question.replaceAll("\\s+", "")
                .replaceAll("[？?。！!]+$", "");
        if (!normalized.endsWith("呢")) {
            return "";
        }
        String subject = normalized.substring(0, normalized.length() - 1);
        if (subject.startsWith("那么")) {
            subject = subject.substring(2);
        } else if (subject.startsWith("那")) {
            subject = subject.substring(1);
        }
        for (String suffix : NAMING_SUBJECT_SUFFIXES) {
            if (subject.equals(suffix)) {
                return suffix;
            }
        }
        return "";
    }

    private static Optional<String> replaceNamingSubject(String previousQuestion, String subject) {
        if (!previousQuestion.contains("命名")) {
            return Optional.empty();
        }
        for (String previousSubject : NAMING_SUBJECT_SUFFIXES) {
            int subjectIndex = previousQuestion.lastIndexOf(previousSubject);
            if (subjectIndex >= 0) {
                return Optional.of(previousQuestion.substring(0, subjectIndex)
                        + subject
                        + previousQuestion.substring(subjectIndex + previousSubject.length()));
            }
        }
        return Optional.empty();
    }

    private static int subjectRuleScore(String text, String subject) {
        if (subject.isBlank()) {
            return 0;
        }
        int score = occurrenceCount(text, subject) * 8;
        Matcher directMatcher = directNamingRulePattern(subject).matcher(text);
        while (directMatcher.find()) {
            score += 320;
        }
        Matcher matcher = namingRulePattern(subject).matcher(text);
        while (matcher.find()) {
            score += 160;
        }
        return score;
    }

    private static boolean hasExplicitNamingRule(String question, String text) {
        String subject = namingSubject(question);
        return !subject.isBlank() && namingRulePattern(subject).matcher(text).find();
    }

    private static int namingRulePriority(String question, String text) {
        String subject = namingSubject(question);
        if (subject.isBlank()) {
            return 0;
        }
        if (directNamingRulePattern(subject).matcher(text).find()) {
            return 2;
        }
        return namingRulePattern(subject).matcher(text).find() ? 1 : 0;
    }

    private static Pattern namingRulePattern(String subject) {
        return Pattern.compile(
                Pattern.quote(subject)
                        + ".{0,16}(?:名为|名称|命名|使用|采用|风格|前缀|后缀|开头|结尾)",
                Pattern.DOTALL
        );
    }

    private static Pattern directNamingRulePattern(String subject) {
        return Pattern.compile(
                Pattern.quote(subject)
                        + "(?:名为|名称|命名|使用|采用|风格|前缀|后缀|开头|结尾)",
                Pattern.DOTALL
        );
    }

    private static int retrievalTopK(String question) {
        return namingSubject(question).isBlank() ? RETRIEVAL_TOP_K : NAMING_RETRIEVAL_TOP_K;
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
