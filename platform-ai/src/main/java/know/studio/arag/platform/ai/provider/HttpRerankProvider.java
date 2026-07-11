package know.studio.arag.platform.ai.provider;

import java.util.List;
import java.util.Set;

public final class HttpRerankProvider implements AiProvider {

    private final RerankProviderClient client;
    private final int priority;

    public HttpRerankProvider(RerankProviderClient client, int priority) {
        this.client = client;
        this.priority = priority;
    }

    @Override
    public String id() {
        return "bge-reranker";
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public Set<AiCapability> capabilities() {
        return Set.of(AiCapability.RERANK);
    }

    @Override
    public List<RerankResult> rerank(String query, List<RerankDocument> documents) {
        return client.rerank(query, documents);
    }
}
