package know.studio.arag.knowledge.infra.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "arag.storage.minio")
@Getter
@Setter
public class KnowledgeStorageProperties {

    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucket = "arag-documents";
    private Duration uploadExpiry = Duration.ofHours(24);
}
