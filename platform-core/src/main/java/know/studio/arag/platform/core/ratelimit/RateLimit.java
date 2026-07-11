package know.studio.arag.platform.core.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流标记。打在方法上，由 {@link RateLimitAspect} 基于 Redisson 分布式限流器执行。
 * 保护模型调用等昂贵入口不被打爆。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流键前缀（为空则用方法全限定名）。 */
    String key() default "";

    /** 时间窗内允许的许可数。 */
    int permits() default 10;

    /** 时间窗（秒）。 */
    int windowSeconds() default 1;

    /** 限流维度：全局 or 按当前用户。 */
    Scope scope() default Scope.USER;

    enum Scope {
        GLOBAL, USER
    }
}
