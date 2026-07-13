package know.studio.ai.provider;

import java.util.List;
import java.util.Set;

public final class OllamaEmbeddingProvider implements AiProvider {

    private final EmbeddingProviderClient client;
    private final int priority;

    public OllamaEmbeddingProvider(EmbeddingProviderClient client, int priority) {
        this.client = client;
        this.priority = priority;
    }

    @Override
    public String id() {
        return "ollama-embedding";
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public Set<AiCapability> capabilities() {
        return Set.of(AiCapability.EMBEDDING);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return client.embed(texts);
    }
}
