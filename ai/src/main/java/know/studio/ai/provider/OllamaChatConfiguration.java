package know.studio.ai.provider;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatProperties;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({OllamaChatProperties.class, OllamaChatFallbackProperties.class})
public class OllamaChatConfiguration {

    @Bean
    @ConditionalOnProperty(
            name = "arag.ai.ollama-chat-fallback.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    AiProvider ollamaChatProvider(
            OllamaApi ollamaApi,
            OllamaChatProperties properties,
            ToolCallingManager toolCallingManager,
            RetryTemplate retryTemplate
    ) {
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(properties.getOptions())
                .toolCallingManager(toolCallingManager)
                .toolExecutionEligibilityPredicate(new DefaultToolExecutionEligibilityPredicate())
                .observationRegistry(ObservationRegistry.NOOP)
                .modelManagementOptions(ModelManagementOptions.defaults())
                .retryTemplate(retryTemplate)
                .build();
        return new OllamaChatProvider(
                new SpringAiChatProviderClient(chatModel),
                20
        );
    }
}
