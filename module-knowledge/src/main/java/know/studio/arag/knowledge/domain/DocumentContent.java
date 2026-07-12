package know.studio.arag.knowledge.domain;

import java.io.InputStream;

public record DocumentContent(
        String fileName,
        String contentType,
        long fileSize,
        InputStream inputStream
) {
}
