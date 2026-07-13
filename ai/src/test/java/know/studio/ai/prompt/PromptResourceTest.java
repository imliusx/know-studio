package know.studio.ai.prompt;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptResourceTest {

    @Test
    void rendersClasspathTemplate() {
        PromptResource prompt = PromptResource.classpath("prompts/test-template.st", "test-v1");

        assertThat(prompt.render(Map.of("name", "KnowStudio"))).isEqualTo("Hello KnowStudio");
        assertThat(prompt.version()).isEqualTo("test-v1");
    }

    @Test
    void rejectsMissingResource() {
        assertThatThrownBy(() -> PromptResource.classpath("prompts/missing.st", "missing-v1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Prompt resource not found");
    }
}
