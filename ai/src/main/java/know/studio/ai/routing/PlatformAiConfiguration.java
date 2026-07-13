package know.studio.ai.routing;

import know.studio.ai.chat.ChatModelRouter;
import know.studio.ai.embedding.EmbeddingClient;
import know.studio.ai.observability.AiObservationSink;
import know.studio.ai.provider.AiProvider;
import know.studio.ai.rerank.RerankClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiRoutingProperties.class)
public class PlatformAiConfiguration {

    @Bean
    ProviderCircuitBreakerRegistry providerCircuitBreakerRegistry(AiRoutingProperties properties) {
        return new ProviderCircuitBreakerRegistry(properties.toConfig());
    }

    @Bean
    ChatModelRouter chatModelRouter(
            List<AiProvider> providers,
            ProviderCircuitBreakerRegistry breakers,
            AiObservationSink observationSink
    ) {
        return new ChatModelRouter(providers, breakers, observationSink);
    }

    @Bean
    EmbeddingClient embeddingClient(
            List<AiProvider> providers,
            ProviderCircuitBreakerRegistry breakers
    ) {
        return new EmbeddingClient(providers, breakers);
    }

    @Bean
    RerankClient rerankClient(
            List<AiProvider> providers,
            ProviderCircuitBreakerRegistry breakers
    ) {
        return new RerankClient(providers, breakers);
    }
}
