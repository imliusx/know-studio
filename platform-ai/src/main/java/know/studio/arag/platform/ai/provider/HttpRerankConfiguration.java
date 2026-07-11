package know.studio.arag.platform.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpRerankProperties.class)
public class HttpRerankConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "arag.ai.rerank", name = "enabled", havingValue = "true")
    AiProvider httpRerankProvider(HttpRerankProperties properties, ObjectMapper objectMapper) {
        return new HttpRerankProvider(
                new HttpRerankProviderClient(
                        objectMapper,
                        properties.getBaseUrl(),
                        properties.getTimeout()
                ),
                properties.getPriority()
        );
    }
}
