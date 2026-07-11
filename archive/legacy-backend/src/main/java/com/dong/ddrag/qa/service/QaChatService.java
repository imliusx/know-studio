package com.dong.ddrag.qa.service;

import com.dong.ddrag.qa.model.KnowledgeAnswerOutput;
import com.dong.ddrag.qa.model.EvidenceLevel;
import com.dong.ddrag.qa.model.vo.AskQuestionResponse;
import com.dong.ddrag.qa.rag.ReadyChunkDocumentRetriever;
import com.dong.ddrag.qa.rag.RetrievedEvidenceBundle;
import com.dong.ddrag.qa.support.CitationAssembler;
import com.dong.ddrag.qa.support.QaAnswerParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * QA 问答的总编排层（对应走读指南「链路 B」）。把"检索证据 → 生成答案"串起来：
 * <pre>
 *   ① retrieveEvidence() 拿到带分级的证据包（详见 HybridChunkRetrievalService）
 *   ② 没证据 → 直接拒答(INSUFFICIENT_EVIDENCE)，连模型都不调用（省成本 + 防幻觉）
 *   ③ 有证据 → 把证据 + 证据分级指导语拼成 prompt，调大模型生成结构化答案
 *   ④ 结构化解析失败 → 降级取纯文本再解析，保证可用性
 * </pre>
 * 关键点：证据分级指导语(evidenceGuidance)会随 prompt 一起送进模型，让它"证据够才答、不够拒答"。
 */
@Service
public class QaChatService {

    private static final Logger log = LoggerFactory.getLogger(QaChatService.class);
    private static final String INSUFFICIENT_CODE = "INSUFFICIENT_EVIDENCE";
    private static final String INSUFFICIENT_MESSAGE = "检索到的有效证据不足，暂不回答。";
    private static final String FORMAT_ERROR_CODE = "ANSWER_FORMAT_ERROR";
    private static final String FORMAT_ERROR_MESSAGE = "模型返回格式错误，无法解析回答。";

    private final ChatClient qaChatClient;
    private final PromptTemplate qaUserPromptTemplate;
    private final ReadyChunkDocumentRetriever documentRetriever;
    private final QaAnswerParser answerParser;
    private final CitationAssembler citationAssembler;

    public QaChatService(
            ChatClient qaChatClient,
            @Qualifier("qaUserPromptTemplate") PromptTemplate qaUserPromptTemplate,
            ReadyChunkDocumentRetriever documentRetriever,
            QaAnswerParser answerParser,
            CitationAssembler citationAssembler
    ) {
        this.qaChatClient = qaChatClient;
        this.qaUserPromptTemplate = qaUserPromptTemplate;
        this.documentRetriever = documentRetriever;
        this.answerParser = answerParser;
        this.citationAssembler = citationAssembler;
    }

    public AskQuestionResponse ask(Long groupId, String question) {
        // ① 检索证据（含 RRF 融合、聚簇、证据分级，见 HybridChunkRetrievalService）
        RetrievedEvidenceBundle evidenceBundle = documentRetriever.retrieveEvidence(groupId, question);
        List<Document> documents = evidenceBundle.documents();
        // ② 证据为空直接拒答——不浪费一次模型调用，也避免无中生有。
        if (documents.isEmpty()) {
            return AskQuestionResponse.unanswered(INSUFFICIENT_CODE, INSUFFICIENT_MESSAGE, List.of());
        }
        // ③ 调大模型生成结构化答案（含 answered 标志，模型可据此"主动拒答"）。
        KnowledgeAnswerOutput output = getStructuredAnswer(groupId, question, evidenceBundle);
        // ④ 结构化解析彻底失败 → 返回格式错误，不把乱码当答案。
        if (output == null) {
            return AskQuestionResponse.unanswered(FORMAT_ERROR_CODE, FORMAT_ERROR_MESSAGE, List.of());
        }
        // ⑤ 模型判定证据不足(answered=false) → 尊重其判断，按拒答处理。
        if (!output.answered() || !StringUtils.hasText(output.answer())) {
            return AskQuestionResponse.unanswered(output.reasonCode(), output.reasonMessage(), List.of());
        }
        // ⑥ 正常作答，并附上可追溯的引用清单(citations)。
        return AskQuestionResponse.answered(
                output.answer().trim(),
                citationAssembler.assembleDocuments(documents)
        );
    }

    private KnowledgeAnswerOutput getStructuredAnswer(
            Long groupId,
            String question,
            RetrievedEvidenceBundle evidenceBundle
    ) {
        Prompt userPrompt = createUserPrompt(question, evidenceBundle);
        try {
            return qaChatClient.prompt(userPrompt)
                    .advisors(advisor -> advisor
                            .param("groupId", groupId)
                            .param(
                                    ReadyChunkDocumentRetriever.PREFETCHED_DOCUMENTS_CONTEXT_KEY,
                                    evidenceBundle.documents()
                            ))
                    .call()
                    .entity(KnowledgeAnswerOutput.class);
        } catch (RuntimeException exception) {
            log.warn(
                    "QA structured output failed, fallback to raw content. groupId={}, evidenceCount={}",
                    groupId,
                    evidenceBundle.documents().size(),
                    exception
            );
            return parseFallbackAnswer(groupId, question, evidenceBundle);
        }
    }

    private KnowledgeAnswerOutput parseFallbackAnswer(
            Long groupId,
            String question,
            RetrievedEvidenceBundle evidenceBundle
    ) {
        Prompt userPrompt = createUserPrompt(question, evidenceBundle);
        try {
            String rawAnswer = qaChatClient.prompt(userPrompt)
                    .advisors(advisor -> advisor
                            .param("groupId", groupId)
                            .param(
                                    ReadyChunkDocumentRetriever.PREFETCHED_DOCUMENTS_CONTEXT_KEY,
                                    evidenceBundle.documents()
                            ))
                    .call()
                    .content();
            log.info(
                    "QA raw answer fallback. groupId={}, evidenceCount={}, rawLength={}",
                    groupId,
                    evidenceBundle.documents().size(),
                    rawAnswer == null ? 0 : rawAnswer.length()
            );
            return answerParser.parse(rawAnswer);
        } catch (RuntimeException exception) {
            log.error(
                    "QA raw answer fallback failed. groupId={}, evidenceCount={}",
                    groupId,
                    evidenceBundle.documents().size(),
                    exception
            );
            return null;
        }
    }

    private Prompt createUserPrompt(String question, RetrievedEvidenceBundle evidenceBundle) {
        EvidenceLevel evidenceLevel = evidenceBundle.evidenceLevel() == null
                ? EvidenceLevel.NONE
                : evidenceBundle.evidenceLevel();
        return qaUserPromptTemplate.create(Map.of(
                "question", question,
                "evidenceLevel", evidenceLevel.name(),
                "evidenceGuidance", evidenceBundle.evidenceGuidance()
        ));
    }
}
