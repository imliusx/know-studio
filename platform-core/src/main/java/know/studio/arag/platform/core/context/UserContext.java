package know.studio.arag.platform.core.context;

/**
 * 请求级用户上下文（ThreadLocal）。持有 userId / traceId，供全链路读取。
 *
 * <p>注意：异步线程池需用 TTL（TransmittableThreadLocal）包装才能透传，
 * 见 platform-core 的线程池配置。请求结束务必 {@link #clear()} 防止内存泄漏。
 */
public final class UserContext {

    private static final ThreadLocal<Principal> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Principal principal) {
        HOLDER.set(principal);
    }

    public static Principal get() {
        return HOLDER.get();
    }

    public static Long userId() {
        Principal p = HOLDER.get();
        return p == null ? null : p.userId();
    }

    public static String traceId() {
        Principal p = HOLDER.get();
        return p == null ? null : p.traceId();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 当前请求主体。
     *
     * @param userId  当前用户 ID
     * @param traceId 链路追踪 ID
     */
    public record Principal(Long userId, String traceId) {
    }
}
