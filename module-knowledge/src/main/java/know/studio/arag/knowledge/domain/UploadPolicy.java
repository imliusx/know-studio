package know.studio.arag.knowledge.domain;

import java.time.Duration;

public record UploadPolicy(Duration sessionExpiry) {

    public UploadPolicy {
        if (sessionExpiry == null || sessionExpiry.isZero() || sessionExpiry.isNegative()) {
            throw new IllegalArgumentException("sessionExpiry must be positive");
        }
    }
}
