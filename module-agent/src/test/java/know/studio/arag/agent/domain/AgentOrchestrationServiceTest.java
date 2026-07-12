package know.studio.arag.agent.domain;

import know.studio.arag.agent.api.ChatRequest;
import know.studio.arag.agent.api.ChatStreamEvent;
import know.studio.arag.agent.api.IntentResult;
import know.studio.arag.agent.api.IntentType;
import know.studio.arag.conversation.api.AppendMessageCommand;
import know.studio.arag.conversation.api.ConversationApi;
import know.studio.arag.conversation.api.ConversationContext;
import know.studio.arag.identity.api.CurrentIdentity;
import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.identity.api.SystemRole;
import know.studio.arag.platform.ai.chat.ChatModelRouter;
import know.studio.arag.platform.ai.provider.ChatChunk;
import know.studio.arag.retrieval.api.Evidence;
import know.studio.arag.retrieval.api.EvidenceBundle;
import know.studio.arag.retrieval.api.EvidenceLevel;
import know.studio.arag.retrieval.api.RetrievalApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

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
        when(conversationApi.loadContextForOwner(userId, sessionId, ""))
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
    void extractsExplicitNamingRuleWithoutModelExpansion() {
        Evidence evidence = new Evidence(
                11L,
                101L,
                1001L,
                7,
                "java-guide.pdf",
                """
                        4. 【强制】类名使用 UpperCamelCase 风格，但以下情形例外：DO / BO / DTO / VO / AO /
                        PO / UID 等。
                        正例：ForceCode / UserDO / HtmlDTO / XmlService
                        反例：forcecode / UserDo / HTMLDto / XMLService
                        5. 【强制】方法名统一使用 lowerCamelCase 风格。
                        """,
                0.9,
                Set.of("KEYWORD")
        );

        assertThat(AgentOrchestrationService.extractNamingRule(
                "Java 类名如何命名？",
                List.of(evidence)
        ).orElseThrow()).isEqualTo("""
                根据知识库规范：

                4. 【强制】类名使用 UpperCamelCase 风格，但以下情形例外：DO / BO / DTO / VO / AO /

                PO / UID 等。

                正例：ForceCode / UserDO / HtmlDTO / XmlService

                反例：forcecode / UserDo / HTMLDto / XMLService
                """.strip());
    }

    private AgentOrchestrationService service() {
        return new AgentOrchestrationService(
                identityApi,
                conversationApi,
                retrievalApi,
                intentClassifier,
                toolRegistry,
                questionDecomposer,
                chatModelRouter
        );
    }
}
