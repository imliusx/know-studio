package know.studio.common.ratelimit;

import know.studio.common.context.UserContext;
import know.studio.common.exception.BusinessException;
import know.studio.common.exception.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * 限流切面。基于 Redisson {@link RRateLimiter} 实现分布式限流（多实例共享），
 * 支持全局与用户级两个维度。超限抛 {@link BusinessException}，由全局异常处理返回可控响应。
 */
@Aspect
@Component
public class RateLimitAspect {

    private final RedissonClient redissonClient;
    private final MeterRegistry meterRegistry;

    public RateLimitAspect(RedissonClient redissonClient, MeterRegistry meterRegistry) {
        this.redissonClient = redissonClient;
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String limiterKey = buildKey(rateLimit, joinPoint);
        RRateLimiter limiter = redissonClient.getRateLimiter(limiterKey);
        limiter.trySetRate(RateType.OVERALL, rateLimit.permits(), rateLimit.windowSeconds(), RateIntervalUnit.SECONDS);
        if (!limiter.tryAcquire(1)) {
            meterRegistry.counter("arag.rate.limit.rejected", "key", rateLimit.key()).increment();
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
        }
        meterRegistry.counter("arag.rate.limit.allowed", "key", rateLimit.key()).increment();
        return joinPoint.proceed();
    }

    private String buildKey(RateLimit rateLimit, ProceedingJoinPoint joinPoint) {
        String base = rateLimit.key().isBlank()
                ? joinPoint.getSignature().toShortString()
                : rateLimit.key();
        if (rateLimit.scope() == RateLimit.Scope.USER) {
            Long userId = UserContext.userId();
            return "rl:" + base + ":u:" + (userId == null ? "anonymous" : userId);
        }
        return "rl:" + base + ":global";
    }
}
