package know.studio.arag.platform.ai.prompt;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class PromptResource {

    private final String version;
    private final String template;

    private PromptResource(String path, String version) {
        this.version = requireText(version, "version");
        this.template = load(path);
    }

    public static PromptResource classpath(String path, String version) {
        return new PromptResource(path, version);
    }

    public String version() {
        return version;
    }

    public String text() {
        return template;
    }

    public String render(Map<String, Object> variables) {
        return PromptTemplate.builder()
                .template(template)
                .build()
                .render(variables == null ? Map.of() : Map.copyOf(variables));
    }

    private static String load(String path) {
        String normalizedPath = requireText(path, "path");
        ClassPathResource resource = new ClassPathResource(normalizedPath);
        if (!resource.exists()) {
            throw new IllegalStateException("Prompt resource not found: " + normalizedPath);
        }
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load prompt resource: " + normalizedPath, exception);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
