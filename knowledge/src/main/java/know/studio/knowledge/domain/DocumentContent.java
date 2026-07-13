package know.studio.knowledge.domain;

import java.io.InputStream;

public record DocumentContent(
        String fileName,
        String contentType,
        long fileSize,
        InputStream inputStream
) {
}
