package know.studio.arag.platform.ai.provider;

import java.util.List;

@FunctionalInterface
public interface RerankProviderClient {

    List<RerankResult> rerank(String query, List<RerankDocument> documents);
}
