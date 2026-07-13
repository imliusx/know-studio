package know.studio.ai.provider;

import java.util.List;

@FunctionalInterface
public interface EmbeddingProviderClient {

    List<float[]> embed(List<String> texts);
}
