package know.studio.ai.provider;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OllamaEmbeddingConfiguration {

    @Bean
    AiProvider ollamaEmbeddingProvider(EmbeddingModel embeddingModel) {
        return new OllamaEmbeddingProvider(
                new SpringAiEmbeddingProviderClient(embeddingModel),
                10
        );
    }
}
