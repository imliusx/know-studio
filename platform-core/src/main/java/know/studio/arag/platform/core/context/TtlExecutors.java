package know.studio.arag.platform.core.context;

import org.slf4j.MDC;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Executor wrappers that propagate request context at task submission time.
 * Both {@link UserContext} and the complete MDC map are restored after each
 * task so pooled threads cannot leak data between requests.
 */
public final class TtlExecutors {

    private TtlExecutors() {
    }

    public static Executor wrap(Executor delegate) {
        Objects.requireNonNull(delegate, "delegate");
        return command -> delegate.execute(ContextSnapshot.capture().wrap(command));
    }

    public static ExecutorService wrap(ExecutorService delegate) {
        return new ContextAwareExecutorService(Objects.requireNonNull(delegate, "delegate"));
    }

    private record ContextSnapshot(UserContext.Principal principal, Map<String, String> mdc) {

        private static ContextSnapshot capture() {
            return new ContextSnapshot(UserContext.get(), MDC.getCopyOfContextMap());
        }

        private Runnable wrap(Runnable command) {
            return () -> {
                ContextSnapshot previous = capture();
                install();
                try {
                    command.run();
                } finally {
                    previous.install();
                }
            };
        }

        private void install() {
            if (principal == null) {
                UserContext.clear();
            } else {
                UserContext.set(principal);
            }
            if (mdc == null || mdc.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(mdc);
            }
        }
    }

    private static final class ContextAwareExecutorService extends AbstractExecutorService {

        private final ExecutorService delegate;

        private ContextAwareExecutorService(ExecutorService delegate) {
            this.delegate = delegate;
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(ContextSnapshot.capture().wrap(command));
        }
    }
}
