package know.studio.arag.platform.ai.provider;

import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OllamaChatConfiguration {

    @Bean
    AiProvider ollamaChatProvider(StreamingChatModel chatModel) {
        return new OllamaChatProvider(
                new SpringAiChatProviderClient(chatModel),
                20
        );
    }
}
