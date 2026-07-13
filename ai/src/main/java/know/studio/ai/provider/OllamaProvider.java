package know.studio.ai.provider;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;

public final class OllamaProvider implements AiProvider {

    private final ChatProviderClient chatClient;
    private final EmbeddingProviderClient embeddingClient;
    private final RerankProviderClient rerankClient;
    private final int priority;

    public OllamaProvider(
            ChatProviderClient chatClient,
            EmbeddingProviderClient embeddingClient,
            RerankProviderClient rerankClient,
            int priority
    ) {
        this.chatClient = chatClient;
        this.embeddingClient = embeddingClient;
        this.rerankClient = rerankClient;
        this.priority = priority;
    }

    @Override
    public String id() {
        return "ollama";
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public Set<AiCapability> capabilities() {
        return Set.of(AiCapability.CHAT, AiCapability.EMBEDDING, AiCapability.RERANK);
    }

    @Override
    public Flux<ChatChunk> streamChat(ChatRequest request) {
        return chatClient.stream(request);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return embeddingClient.embed(texts);
    }

    @Override
    public List<RerankResult> rerank(String query, List<RerankDocument> documents) {
        return rerankClient.rerank(query, documents);
    }
}
