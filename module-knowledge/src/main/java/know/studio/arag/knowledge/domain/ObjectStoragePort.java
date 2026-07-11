package know.studio.arag.knowledge.domain;

import java.io.InputStream;
import java.util.List;

public interface ObjectStoragePort {

    void put(String objectKey, InputStream inputStream, long size, String contentType);

    InputStream open(String objectKey);

    void compose(String targetObjectKey, List<String> sourceObjectKeys);

    void delete(List<String> objectKeys);
}
