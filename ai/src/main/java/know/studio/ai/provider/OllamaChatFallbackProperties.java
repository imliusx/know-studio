package know.studio.ai.provider;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("arag.ai.ollama-chat-fallback")
public class OllamaChatFallbackProperties {

    private boolean enabled = true;
}
