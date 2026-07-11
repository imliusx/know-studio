package know.studio.arag.platform.core.trace;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

class RagTraceAspectTest {

    @Test
    void reactiveTraceCompletesWithPublisherLifecycle() throws Throwable {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RagTraceAspect aspect = new RagTraceAspect(OpenTelemetrySdk.builder().build(), registry);
        MethodSignature signature = proxy(MethodSignature.class, (method, arguments) -> switch (method.getName()) {
            case "getReturnType" -> Flux.class;
            case "toShortString" -> "Test.stream()";
            default -> defaultValue(method.getReturnType());
        });
        ProceedingJoinPoint joinPoint = proxy(ProceedingJoinPoint.class, (method, arguments) ->
                switch (method.getName()) {
                    case "getSignature" -> signature;
                    case "proceed" -> Flux.just("a", "b");
                    case "getArgs" -> new Object[0];
                    default -> defaultValue(method.getReturnType());
                });
        RagTraceNode node = node();

        Object traced = aspect.around(joinPoint, node);

        assertThat(registry.find("arag.trace.node").timer()).isNull();
        StepVerifier.create((Flux<?>) traced).expectNextCount(2).verifyComplete();
        assertThat(registry.timer(
                "arag.trace.node",
                "step", "test.reactive",
                "status", "success"
        ).count()).isEqualTo(1);
    }

    @Test
    void reactiveTraceDoesNotLeakThreadLocalWhileStreamIsOpen() throws Throwable {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RagTraceAspect aspect = new RagTraceAspect(OpenTelemetrySdk.builder().build(), registry);
        MethodSignature signature = proxy(MethodSignature.class, (method, arguments) ->
                method.getName().equals("getReturnType") ? Flux.class : defaultValue(method.getReturnType()));
        ProceedingJoinPoint joinPoint = proxy(ProceedingJoinPoint.class, (method, arguments) ->
                switch (method.getName()) {
                    case "getSignature" -> signature;
                    case "proceed" -> Flux.never();
                    case "getArgs" -> new Object[0];
                    default -> defaultValue(method.getReturnType());
                });

        Disposable subscription = ((Flux<?>) aspect.around(joinPoint, node())).subscribe();

        assertThat(TraceContext.current()).isNull();
        subscription.dispose();
    }

    private static RagTraceNode node() {
        return new RagTraceNode() {
            @Override
            public String value() {
                return "test.reactive";
            }

            @Override
            public boolean captureInput() {
                return false;
            }

            @Override
            public boolean captureOutput() {
                return false;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return RagTraceNode.class;
            }
        };
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, arguments) -> invocation.invoke(method, arguments)
        ));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        return 0;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(java.lang.reflect.Method method, Object[] arguments) throws Throwable;
    }
}
