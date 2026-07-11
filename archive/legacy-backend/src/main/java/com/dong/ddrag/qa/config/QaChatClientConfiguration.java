package com.dong.ddrag.qa.config;

import com.dong.ddrag.qa.rag.ReadyChunkDocumentRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Spring AI 装配中心（对应走读指南「链路 B」）——把 RAG 各组件组装成可用的 {@link ChatClient}。
 *
 * <p>核心是 {@link RetrievalAugmentationAdvisor}：它把"检索(RAG)"和"生成(模型)"粘在一起。
 * 这里挂了自定义的 {@link ReadyChunkDocumentRetriever}（混合检索）+ {@link ContextualQueryAugmenter}
 * （把检索到的证据拼进 prompt）。QaChatService 拿到的 qaChatClient 已内置这套 RAG 增强能力。
 *
 * <p>三类 PromptTemplate Bean 分别对应：系统提示词、用户提示词、RAG 上下文拼接模板（都在 resources/prompts/qa/）。
 */
@Configuration
public class QaChatClientConfiguration {

    @Bean
    public ChatClient qaChatClient(
            ChatClient.Builder chatClientBuilder,
            @Qualifier("qaSystemPromptTemplate") PromptTemplate qaSystemPromptTemplate,
            @Qualifier("qaRetrievalAdvisor") RetrievalAugmentationAdvisor qaRetrievalAdvisor
    ) {
        return chatClientBuilder
                .defaultSystem(qaSystemPromptTemplate.getTemplate())
                .defaultAdvisors(qaRetrievalAdvisor)
                .build();
    }

    @Bean
    public PromptTemplate qaSystemPromptTemplate() {
        return PromptTemplate.builder()
                .resource(new ClassPathResource("prompts/qa/system.st"))
                .build();
    }

    @Bean
    public PromptTemplate qaUserPromptTemplate() {
        return PromptTemplate.builder()
                .resource(new ClassPathResource("prompts/qa/user.st"))
                .build();
    }

    @Bean
    public PromptTemplate qaRagContextPromptTemplate() {
        return PromptTemplate.builder()
                .resource(new ClassPathResource("prompts/qa/rag-context.st"))
                .build();
    }

    @Bean("qaRetrievalAdvisor")
    public RetrievalAugmentationAdvisor qaRetrievalAdvisor(
            ReadyChunkDocumentRetriever readyChunkDocumentRetriever,
            @Qualifier("qaRagContextPromptTemplate") PromptTemplate qaRagContextPromptTemplate
    ) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(readyChunkDocumentRetriever)
                .queryAugmenter(new ContextualQueryAugmenter.Builder()
                        .allowEmptyContext(true)
                        .promptTemplate(qaRagContextPromptTemplate)
                        .build())
                .build();
    }
}
