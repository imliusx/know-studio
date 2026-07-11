package know.studio.arag.platform.ai.provider;

import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.Objects;

/** Adapts a Spring AI embedding model to the provider-neutral SPI. */
public final class SpringAiEmbeddingProviderClient implements EmbeddingProviderClient {

    private final EmbeddingModel model;

    public SpringAiEmbeddingProviderClient(EmbeddingModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return model.embed(texts);
    }
}
