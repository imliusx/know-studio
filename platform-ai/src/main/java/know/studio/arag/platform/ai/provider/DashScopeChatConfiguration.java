package know.studio.arag.platform.ai.provider;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DashScopeChatConfiguration {

    @Bean
    AiProvider dashScopeChatProvider(DashScopeChatModel chatModel) {
        return new DashScopeChatProvider(new SpringAiChatProviderClient(chatModel), 10);
    }
}
