package know.studio.agent.api;

import reactor.core.publisher.Flux;

public interface AgentApi {

    Flux<ChatStreamEvent> streamChat(ChatRequest request);
}
