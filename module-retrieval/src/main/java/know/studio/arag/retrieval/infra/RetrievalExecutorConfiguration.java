package know.studio.arag.retrieval.infra;

import know.studio.arag.platform.core.context.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
public class RetrievalExecutorConfiguration {

    @Bean(destroyMethod = "shutdown")
    ExecutorService retrievalExecutor() {
        return TtlExecutors.wrap(Executors.newVirtualThreadPerTaskExecutor());
    }
}
