package know.studio.knowledge.domain;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface InputStreamProvider {

    InputStream open() throws IOException;
}
