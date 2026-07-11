package know.studio.arag.platform.core.context;

import know.studio.arag.platform.core.trace.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class TtlExecutorsTest {

    @AfterEach
    void clearContext() {
        UserContext.clear();
        TraceContext.clear();
    }

    @Test
    void propagatesContextAndRestoresWorkerThread() throws Exception {
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ExecutorService executor = TtlExecutors.wrap(delegate);
        try {
            UserContext.set(new UserContext.Principal(10L, 20L, "trace-a"));
            TraceContext.set("trace-a");

            Future<ContextValues> propagated = executor.submit(() -> new ContextValues(
                    UserContext.userId(),
                    UserContext.workspaceId(),
                    TraceContext.current()
            ));

            assertThat(propagated.get()).isEqualTo(new ContextValues(10L, 20L, "trace-a"));

            UserContext.clear();
            TraceContext.clear();
            Future<ContextValues> cleared = executor.submit(() -> new ContextValues(
                    UserContext.userId(),
                    UserContext.workspaceId(),
                    TraceContext.current()
            ));

            assertThat(cleared.get()).isEqualTo(new ContextValues(null, null, null));
        } finally {
            executor.shutdownNow();
        }
    }

    private record ContextValues(Long userId, Long workspaceId, String traceId) {
    }
}
