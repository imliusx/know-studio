package know.studio.knowledge.infra.storage;

import io.minio.MinioClient;
import know.studio.knowledge.domain.UploadPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KnowledgeStorageProperties.class)
public class KnowledgeStorageConfiguration {

    @Bean
    MinioClient minioClient(KnowledgeStorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    UploadPolicy uploadPolicy(KnowledgeStorageProperties properties) {
        return new UploadPolicy(properties.getUploadExpiry());
    }
}
