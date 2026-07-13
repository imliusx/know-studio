package know.studio.agent.domain;

import know.studio.agent.api.ChatRequest;
import know.studio.agent.api.ChatStreamEvent;
import know.studio.agent.api.IntentResult;
import know.studio.agent.api.IntentType;
import know.studio.agent.prompt.AgentPromptCatalog;
import know.studio.chat.api.AppendMessageCommand;
import know.studio.chat.api.ConversationApi;
import know.studio.chat.api.ConversationContext;
import know.studio.chat.api.ConversationMessage;
import know.studio.chat.api.MessageRole;
import know.studio.auth.api.CurrentIdentity;
import know.studio.auth.api.IdentityApi;
import know.studio.auth.api.SystemRole;
import know.studio.ai.chat.ChatModelRouter;
import know.studio.ai.provider.ChatChunk;
import know.studio.ai.provider.ChatMessageRole;
import know.studio.ai.provider.GenerationProfile;
import know.studio.search.api.Evidence;
import know.studio.search.api.EvidenceBundle;
import know.studio.search.api.EvidenceLevel;
import know.studio.search.api.RetrievalApi;
import know.studio.search.api.RetrievalQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentOrchestrationServiceTest {

    @Mock
    private IdentityApi identityApi;
    @Mock
    private ConversationApi conversationApi;
    @Mock
    private RetrievalApi retrievalApi;
    @Mock
    private IntentClassifier intentClassifier;
    @Mock
    private AgentToolRegistry toolRegistry;
    @Mock
    private QuestionDecomposer questionDecomposer;
    @Mock
    private ChatModelRouter chatModelRouter;

    @Test
    void persistsKnowledgeBaseAndCitationMetadataFromKnowledgeAnswer() {
        long userId = 20L;
        long knowledgeBaseId = 11L;
        long sessionId = 100L;
        when(identityApi.currentUser()).thenReturn(new CurrentIdentity(
                userId,
                "user@example.com",
                "User",
                SystemRole.USER
        ));
        when(conversationApi.loadContextForOwner(userId, sessionId, "What is RAG?"))
                .thenReturn(new ConversationContext(sessionId, "", "", List.of(), ""));
        when(intentClassifier.classify("What is RAG?", false))
                .thenReturn(new IntentResult(IntentType.KNOWLEDGE, 0.9, ""));
        when(retrievalApi.retrieve(any())).thenReturn(new EvidenceBundle(
                List.of(new Evidence(
                        knowledgeBaseId,
                        101L,
                        1001L,
                        0,
                        "guide.md",
                        "RAG combines retrieval and generation.",
                        0.9,
                        Set.of("VECTOR")
                )),
                EvidenceLevel.SUFFICIENT,
                ""
        ));
        when(chatModelRouter.stream(any())).thenReturn(Flux.just(ChatChunk.token("answer")));

        List<ChatStreamEvent> events = service().streamChat(new ChatRequest(
                sessionId,
                "What is RAG?",
                Set.of(knowledgeBaseId),
                false,
                false
        )).collectList().block();

        assertThat(events).extracting(ChatStreamEvent::type).containsExactly(
                ChatStreamEvent.Type.CITATION,
                ChatStreamEvent.Type.TOKEN,
                ChatStreamEvent.Type.DONE
        );
        Map<?, ?> citationEvent = (Map<?, ?>) events.getFirst().payload();
        assertThat(citationEvent.get("knowledgeBaseId")).isEqualTo(Long.toString(knowledgeBaseId));
        assertThat(citationEvent.get("chunkIndex")).isEqualTo(0);
        assertThat(citationEvent.get("snippet")).isEqualTo("RAG combines retrieval and generation.");

        ArgumentCaptor<AppendMessageCommand> commandCaptor = ArgumentCaptor.forClass(AppendMessageCommand.class);
        verify(conversationApi, org.mockito.Mockito.times(2))
                .appendMessageForOwner(commandCaptor.capture(), eq(userId));
        AppendMessageCommand assistant = commandCaptor.getAllValues().getLast();
        assertThat(assistant.metadata()).containsEntry("knowledgeBaseIds", List.of(Long.toString(knowledgeBaseId)));
        List<?> citations = (List<?>) assistant.metadata().get("citations");
        assertThat(citations).hasSize(1);
        Map<?, ?> persistedCitation = (Map<?, ?>) citations.getFirst();
        assertThat(persistedCitation.get("documentId")).isEqualTo("101");
        assertThat(persistedCitation.get("chunkIndex")).isEqualTo(0);
        assertThat(persistedCitation.get("snippet")).isEqualTo("RAG combines retrieval and generation.");

        ArgumentCaptor<know.studio.ai.provider.ChatRequest> aiRequestCaptor =
                ArgumentCaptor.forClass(know.studio.ai.provider.ChatRequest.class);
        verify(chatModelRouter).stream(aiRequestCaptor.capture());
        assertThat(aiRequestCaptor.getValue().profile()).isEqualTo(GenerationProfile.KNOWLEDGE);
        assertThat(aiRequestCaptor.getValue().promptVersion()).isEqualTo("knowledge-v3");
        assertThat(aiRequestCaptor.getValue().systemPrompt()).contains("只能使用与问题直接相关的证据");
    }

    @Test
    void generatesExplicitNamingRuleThroughKnowledgeModel() {
        long userId = 20L;
        long knowledgeBaseId = 11L;
        long sessionId = 100L;
        String question = "Java 索引如何命名？";
        when(identityApi.currentUser()).thenReturn(new CurrentIdentity(
                userId,
                "user@example.com",
                "User",
                SystemRole.USER
        ));
        when(conversationApi.loadContextForOwner(userId, sessionId, question))
                .thenReturn(new ConversationContext(sessionId, "", "", List.of(), ""));
        when(intentClassifier.classify(question, false))
                .thenReturn(new IntentResult(IntentType.KNOWLEDGE, 0.95, ""));
        when(retrievalApi.retrieve(any())).thenReturn(new EvidenceBundle(
                List.of(new Evidence(
                        knowledgeBaseId,
                        101L,
                        1001L,
                        62,
                        "java-guide.pdf",
                        "主键索引名为 pk_字段名；唯一索引名为 uk_字段名；普通索引名为 idx_字段名。",
                        0.9,
                        Set.of("KEYWORD")
                )),
                EvidenceLevel.SUFFICIENT,
                ""
        ));
        String naturalAnswer = "索引命名规则为：主键使用 `pk_`，唯一索引使用 `uk_`，普通索引使用 `idx_`。";
        when(chatModelRouter.stream(any())).thenReturn(Flux.just(ChatChunk.token(naturalAnswer)));

        List<ChatStreamEvent> events = service().streamChat(new ChatRequest(
                sessionId,
                question,
                Set.of(knowledgeBaseId),
                false,
                false
        )).collectList().block();

        assertThat(events).extracting(ChatStreamEvent::type).containsExactly(
                ChatStreamEvent.Type.CITATION,
                ChatStreamEvent.Type.TOKEN,
                ChatStreamEvent.Type.DONE
        );
        assertThat(events.get(1).payload()).isEqualTo(naturalAnswer);
        ArgumentCaptor<know.studio.ai.provider.ChatRequest> requestCaptor =
                ArgumentCaptor.forClass(know.studio.ai.provider.ChatRequest.class);
        verify(chatModelRouter).stream(requestCaptor.capture());
        assertThat(requestCaptor.getValue().profile()).isEqualTo(GenerationProfile.KNOWLEDGE);
        assertThat(requestCaptor.getValue().promptVersion()).isEqualTo("knowledge-v3");
        assertThat(requestCaptor.getValue().userPrompt())
                .contains("pk_字段名")
                .contains("uk_字段名")
                .contains("idx_字段名");
    }

    @Test
    void usesNaturalChatPromptAndPreservesTypedHistoryWithoutDuplicatingCurrentQuestion() {
        long userId = 20L;
        long sessionId = 100L;
        when(identityApi.currentUser()).thenReturn(new CurrentIdentity(
                userId,
                "user@example.com",
                "User",
                SystemRole.USER
        ));
        when(conversationApi.loadContextForOwner(userId, sessionId, "请自然地介绍一下自己"))
                .thenReturn(new ConversationContext(
                        sessionId,
                        "用户正在了解系统能力",
                        "",
                        List.of(
                                message(1L, MessageRole.USER, "你能做什么？"),
                                message(2L, MessageRole.ASSISTANT, "我可以回答知识库问题。"),
                                message(3L, MessageRole.USER, "请自然地介绍一下自己")
                        ),
                        ""
                ));
        when(intentClassifier.classify("请自然地介绍一下自己", false))
                .thenReturn(new IntentResult(IntentType.CHAT, 0.95, ""));
        when(chatModelRouter.stream(any())).thenReturn(Flux.just(ChatChunk.token("我是 KnowStudio 助手。")));

        service().streamChat(new ChatRequest(
                sessionId,
                "请自然地介绍一下自己",
                Set.of(),
                false,
                false
        )).collectList().block();

        ArgumentCaptor<know.studio.ai.provider.ChatRequest> requestCaptor =
                ArgumentCaptor.forClass(know.studio.ai.provider.ChatRequest.class);
        verify(chatModelRouter).stream(requestCaptor.capture());
        know.studio.ai.provider.ChatRequest request = requestCaptor.getValue();
        assertThat(request.profile()).isEqualTo(GenerationProfile.CHAT);
        assertThat(request.promptVersion()).isEqualTo("chat-v1");
        assertThat(request.systemPrompt())
                .contains("自然交流")
                .doesNotContain("只能使用与问题直接相关的证据");
        assertThat(request.history()).extracting(item -> item.role()).containsExactly(
                ChatMessageRole.SYSTEM,
                ChatMessageRole.USER,
                ChatMessageRole.ASSISTANT
        );
        assertThat(request.history()).extracting(item -> item.content())
                .doesNotContain("请自然地介绍一下自己");
        assertThat(request.userPrompt()).isEqualTo("请自然地介绍一下自己");
    }

    @Test
    void resolvesNamingFollowUpBeforeClassificationRetrievalAndGeneration() {
        long userId = 20L;
        long sessionId = 100L;
        String followUp = "那常量呢？";
        String resolved = "如何命名 Java 的常量，给出示例代码。";
        when(identityApi.currentUser()).thenReturn(new CurrentIdentity(
                userId,
                "user@example.com",
                "User",
                SystemRole.USER
        ));
        ConversationContext context = new ConversationContext(
                sessionId,
                "",
                "",
                List.of(
                        message(1L, MessageRole.USER, "如何命名 Java 的类名，给出示例代码。"),
                        message(2L, MessageRole.ASSISTANT, "类名使用 UpperCamelCase。"),
                        message(3L, MessageRole.USER, followUp)
                ),
                followUp
        );
        when(conversationApi.loadContextForOwner(userId, sessionId, followUp)).thenReturn(context);
        when(intentClassifier.classify(resolved, false))
                .thenReturn(new IntentResult(IntentType.KNOWLEDGE, 0.95, ""));
        when(retrievalApi.retrieve(any())).thenReturn(new EvidenceBundle(
                List.of(evidence(
                        8L,
                        "常量命名全部大写，单词间用下划线隔开。正例：MAX_STOCK_COUNT。"
                )),
                EvidenceLevel.SUFFICIENT,
                ""
        ));
        when(chatModelRouter.stream(any())).thenReturn(Flux.just(ChatChunk.token(
                "常量使用全大写下划线命名，例如 `MAX_STOCK_COUNT`。"
        )));

        service().streamChat(new ChatRequest(
                sessionId,
                followUp,
                Set.of(11L),
                false,
                false
        )).collectList().block();

        verify(intentClassifier).classify(resolved, false);
        ArgumentCaptor<RetrievalQuery> retrievalCaptor =
                ArgumentCaptor.forClass(RetrievalQuery.class);
        verify(retrievalApi).retrieve(retrievalCaptor.capture());
        assertThat(retrievalCaptor.getValue().question()).isEqualTo(resolved);
        assertThat(retrievalCaptor.getValue().topK()).isEqualTo(20);

        ArgumentCaptor<know.studio.ai.provider.ChatRequest> aiRequestCaptor =
                ArgumentCaptor.forClass(know.studio.ai.provider.ChatRequest.class);
        verify(chatModelRouter).stream(aiRequestCaptor.capture());
        assertThat(aiRequestCaptor.getValue().userPrompt())
                .contains(resolved)
                .contains("MAX_STOCK_COUNT");
        assertThat(aiRequestCaptor.getValue().history()).extracting(item -> item.content())
                .contains("如何命名 Java 的类名，给出示例代码。")
                .doesNotContain(followUp);

        ArgumentCaptor<AppendMessageCommand> messageCaptor = ArgumentCaptor.forClass(AppendMessageCommand.class);
        verify(conversationApi, org.mockito.Mockito.times(2))
                .appendMessageForOwner(messageCaptor.capture(), eq(userId));
        assertThat(messageCaptor.getAllValues().getFirst().content()).isEqualTo(followUp);
    }

    @Test
    void leavesUnrelatedShortFollowUpUnchanged() {
        ConversationContext context = new ConversationContext(
                100L,
                "",
                "",
                List.of(
                        message(1L, MessageRole.USER, "如何命名 Java 的类名？"),
                        message(2L, MessageRole.USER, "这个呢？")
                ),
                "这个呢？"
        );

        assertThat(AgentOrchestrationService.contextualQuestion(context, "这个呢？"))
                .isEqualTo("这个呢？");
    }

    @Test
    void focusesLongEvidenceAroundQuestionTerms() {
        String evidence = "x".repeat(1_500)
                + "类名使用 UpperCamelCase 风格，正例为 UserService。"
                + "y".repeat(1_500);

        String focused = AgentOrchestrationService.focusEvidence("Java 类名如何命名？", evidence);

        assertThat(focused).contains("类名使用 UpperCamelCase");
        assertThat(focused.length()).isLessThanOrEqualTo(702);
    }

    @Test
    void ranksExplicitSubjectRuleAboveGeneralNamingAdvice() {
        int explicitRule = AgentOrchestrationService.evidenceRelevance(
                "Java 类名如何命名？",
                "类名使用 UpperCamelCase 风格，正例为 UserService。"
        );
        int generalAdvice = AgentOrchestrationService.evidenceRelevance(
                "Java 类名如何命名？",
                "为了代码自解释，命名时应使用完整单词。常量和变量的命名应表达类型。"
        );

        assertThat(explicitRule).isGreaterThan(generalAdvice);
    }

    @Test
    void normalizesEquivalentNamingQuestionSubjects() {
        assertThat(AgentOrchestrationService.namingSubject("索引如何命名？")).isEqualTo("索引");
        assertThat(AgentOrchestrationService.namingSubject("Java 的索引如何命名？")).isEqualTo("索引");
        assertThat(AgentOrchestrationService.namingSubject("Java 中的索引如何命名？")).isEqualTo("索引");
        assertThat(AgentOrchestrationService.namingSubject("数据库索引怎么命名？")).isEqualTo("索引");
        assertThat(AgentOrchestrationService.namingSubject("如何命名 Java 的类名？")).isEqualTo("类名");
    }

    @Test
    void ranksDirectConstantRuleAboveRelatedNamingAdvice() {
        int directRule = AgentOrchestrationService.evidenceRelevance(
                "Java 的常量如何命名？",
                "常量命名全部大写，单词间用下划线隔开。正例：MAX_STOCK_COUNT。"
        );
        int relatedAdvice = AgentOrchestrationService.evidenceRelevance(
                "Java 的常量如何命名？",
                "在常量与变量的命名时，表示类型的名词放在词尾，例如 TERMINATED_THREAD_COUNT。"
        );

        assertThat(directRule).isGreaterThan(relatedAdvice);
    }

    @Test
    void selectsDirectConstantRuleBeforeRelatedAdviceWithHigherTermCoverage() {
        Evidence directRule = evidence(
                8L,
                "常量命名全部大写，单词间用下划线隔开。"
                        + "正例：MAX_STOCK_COUNT / CACHE_EXPIRED_TIME。"
        );
        Evidence relatedAdvice = evidence(
                10L,
                "Java 常量与变量的命名时，表示类型的名词放在词尾，"
                        + "例如 TERMINATED_THREAD_COUNT。"
        );

        assertThat(AgentOrchestrationService.groundingEvidence(
                "Java 的常量如何命名？",
                List.of(relatedAdvice, directRule)
        )).containsExactly(directRule);
    }

    @Test
    void keepsOnlyEvidenceThatCoversTheCurrentQuestion() {
        Evidence direct = evidence(
                1L,
                "Service 和 DAO 层获取单个对象的方法用 get 前缀，获取多个对象用 list 前缀。"
        );
        Evidence glossary = evidence(
                2L,
                "DTO 是数据传输对象，Service 层负责输出业务对象。"
        );

        assertThat(AgentOrchestrationService.groundingEvidence(
                "在 Service 和 DAO 层，获取单个对象和多个对象的方法分别使用什么前缀？",
                List.of(glossary, direct)
        )).containsExactly(direct);
    }

    @Test
    void ranksSemanticCoverageAboveRepeatedGenericTerms() {
        Evidence direct = evidence(
                1L,
                "Service/DAO 层方法命名规约：获取单个对象的方法用 get 做前缀；"
                        + "获取多个对象的方法用 list 做前缀。"
        );
        Evidence repeatedButUnrelated = evidence(
                2L,
                ("Service 和 DAO 的 getter/setter 方法应放在类体最后。")
                        .repeat(12)
        );

        assertThat(AgentOrchestrationService.groundingEvidence(
                "Service/DAO 层获取单个对象和多个对象的方法分别使用什么前缀？",
                List.of(repeatedButUnrelated, direct)
        )).containsExactly(direct);
    }

    @Test
    void prefersIndexNamingRuleOverIndexLengthRule() {
        Evidence namingRule = evidence(
                1L,
                "主键索引名为 pk_字段名；唯一索引名为 uk_字段名；普通索引名为 idx_字段名。"
        );
        Evidence indexLengthRule = evidence(
                2L,
                "在 varchar 字段上建立索引时，必须指定索引长度。".repeat(12)
        );

        assertThat(AgentOrchestrationService.groundingEvidence(
                "Java 的索引如何命名？",
                List.of(indexLengthRule, namingRule)
        )).containsExactly(namingRule);
    }

    @Test
    void focusesIndexNamingRuleAndLeavesNaturalWordingToModel() {
        String evidence = "主键索引名为 pk_字段名；唯一索引名为 uk_字段名；普通索引名为 idx_字段名。"
                + "x".repeat(1_200)
                + "在 varchar 字段上建立索引时，必须指定索引长度。";

        String focused = AgentOrchestrationService.focusEvidence("Java 索引如何命名？", evidence);

        assertThat(focused)
                .contains("主键索引名为 pk_字段名")
                .doesNotContain("varchar 字段上建立索引");
    }

    private AgentOrchestrationService service() {
        return new AgentOrchestrationService(
                identityApi,
                conversationApi,
                retrievalApi,
                intentClassifier,
                toolRegistry,
                questionDecomposer,
                chatModelRouter,
                new AgentPromptCatalog()
        );
    }

    private static ConversationMessage message(long id, MessageRole role, String content) {
        return new ConversationMessage(id, role, content, content.length(), Map.of(), Instant.EPOCH);
    }

    private static Evidence evidence(long chunkId, String text) {
        return new Evidence(11L, 101L, chunkId, 0, "guide.pdf", text, 0.9, Set.of("KEYWORD"));
    }
}
