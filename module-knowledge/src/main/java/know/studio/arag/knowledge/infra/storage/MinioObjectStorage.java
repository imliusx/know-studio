package know.studio.arag.knowledge.infra.storage;

import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import know.studio.arag.knowledge.domain.ObjectStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MinioObjectStorage implements ObjectStoragePort {

    private final MinioClient minioClient;
    private final KnowledgeStorageProperties properties;

    @PostConstruct
    void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getBucket())
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(properties.getBucket())
                        .build());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize MinIO bucket", exception);
        }
    }

    @Override
    public void put(String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType == null || contentType.isBlank()
                            ? "application/octet-stream"
                            : contentType)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to store object " + objectKey, exception);
        }
    }

    @Override
    public InputStream open(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to open object " + objectKey, exception);
        }
    }

    @Override
    public void compose(String targetObjectKey, List<String> sourceObjectKeys) {
        try {
            List<ComposeSource> sources = sourceObjectKeys.stream()
                    .map(objectKey -> ComposeSource.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build())
                    .toList();
            minioClient.composeObject(ComposeObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(targetObjectKey)
                    .sources(sources)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to compose object " + targetObjectKey, exception);
        }
    }

    @Override
    public void delete(List<String> objectKeys) {
        for (String objectKey : objectKeys) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectKey)
                        .build());
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to remove object " + objectKey, exception);
            }
        }
    }
}
