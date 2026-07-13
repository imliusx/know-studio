package know.studio.ai.provider;

import java.util.List;

@FunctionalInterface
public interface RerankProviderClient {

    List<RerankResult> rerank(String query, List<RerankDocument> documents);
}
