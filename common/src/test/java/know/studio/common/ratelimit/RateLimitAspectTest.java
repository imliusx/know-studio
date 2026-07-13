package know.studio.common.ratelimit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import know.studio.common.exception.BusinessException;
import know.studio.common.exception.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitAspectTest {

    @Test
    void rejectionUsesTooManyRequestsAndRecordsMetric() throws Throwable {
        RRateLimiter limiter = proxy(RRateLimiter.class, (method, arguments) -> switch (method.getName()) {
            case "tryAcquire" -> false;
            case "trySetRate" -> true;
            default -> defaultValue(method.getReturnType());
        });
        RedissonClient redisson = proxy(RedissonClient.class, (method, arguments) ->
                method.getName().equals("getRateLimiter") ? limiter : defaultValue(method.getReturnType()));
        ProceedingJoinPoint joinPoint = proxy(ProceedingJoinPoint.class, (method, arguments) -> {
            if (method.getName().equals("proceed")) {
                throw new AssertionError("rejected invocation must not proceed");
            }
            return defaultValue(method.getReturnType());
        });
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RateLimit annotation = annotation();
        RateLimitAspect aspect = new RateLimitAspect(redisson, registry);

        assertThatThrownBy(() -> aspect.around(joinPoint, annotation))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
        assertThat(registry.counter("arag.rate.limit.rejected", "key", "test").count()).isEqualTo(1);
    }

    private static RateLimit annotation() {
        return new RateLimit() {
            @Override
            public String key() {
                return "test";
            }

            @Override
            public int permits() {
                return 1;
            }

            @Override
            public int windowSeconds() {
                return 1;
            }

            @Override
            public Scope scope() {
                return Scope.USER;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return RateLimit.class;
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
