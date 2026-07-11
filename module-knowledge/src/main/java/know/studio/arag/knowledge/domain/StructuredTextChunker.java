package know.studio.arag.knowledge.domain;

import know.studio.arag.platform.core.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StructuredTextChunker {

    private static final int MAX_CHARS = 1_000;
    private static final int OVERLAP_CHARS = 120;

    private final SnowflakeIdGenerator idGenerator;

    public List<DocumentChunk> chunk(long workspaceId, long documentId, String source) {
        String text = clean(source);
        List<DocumentChunk> chunks = new ArrayList<>();
        String section = "正文";
        int cursor = 0;
        while (cursor < text.length()) {
            int end = chooseEnd(text, cursor);
            String value = text.substring(cursor, end).trim();
            if (isHeading(value)) {
                section = headingText(value);
            } else if (!value.isBlank()) {
                chunks.add(new DocumentChunk(
                        idGenerator.nextId(),
                        workspaceId,
                        documentId,
                        chunks.size(),
                        value,
                        cursor,
                        end,
                        section
                ));
            }
            if (end >= text.length()) {
                break;
            }
            cursor = Math.max(cursor + 1, end - OVERLAP_CHARS);
        }
        return List.copyOf(chunks);
    }

    static String clean(String source) {
        if (source == null) {
            return "";
        }
        return source.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("[ ]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static int chooseEnd(String text, int start) {
        int hardEnd = Math.min(text.length(), start + MAX_CHARS);
        if (hardEnd == text.length()) {
            return hardEnd;
        }
        int paragraphEnd = text.lastIndexOf("\n\n", hardEnd);
        if (paragraphEnd > start + MAX_CHARS / 2) {
            return paragraphEnd + 2;
        }
        int sentenceEnd = Math.max(text.lastIndexOf('。', hardEnd), text.lastIndexOf('.', hardEnd));
        if (sentenceEnd > start + MAX_CHARS / 2) {
            return sentenceEnd + 1;
        }
        return hardEnd;
    }

    private static boolean isHeading(String text) {
        return text.startsWith("#") && !text.contains("\n");
    }

    private static String headingText(String heading) {
        String value = heading.replaceFirst("^#+\\s*", "").trim();
        return value.isBlank() ? "正文" : value;
    }
}
