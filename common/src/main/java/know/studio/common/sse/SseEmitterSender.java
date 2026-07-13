package know.studio.common.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 线程安全的 SSE 发送封装。
 *
 * <p>用 {@link AtomicBoolean} 守护：客户端断开/超时/出错后不再写事件，
 * 避免把模型侧异常放大成二次 SSE 错误。所有流式接口（chat/stream）统一用它。
 */
public class SseEmitterSender {

    private final SseEmitter emitter;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SseEmitterSender(long timeoutMillis) {
        this.emitter = new SseEmitter(timeoutMillis);
        this.emitter.onCompletion(() -> closed.set(true));
        this.emitter.onTimeout(() -> closed.set(true));
        this.emitter.onError(e -> closed.set(true));
    }

    public SseEmitter emitter() {
        return emitter;
    }

    /** 发送一个命名事件；客户端已关闭则静默跳过。 */
    public synchronized void send(String event, Object data) {
        if (closed.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException | IllegalStateException ex) {
            complete();
        }
    }

    public void complete() {
        if (closed.compareAndSet(false, true)) {
            emitter.complete();
        }
    }

    public void completeWithError(Throwable throwable) {
        if (closed.compareAndSet(false, true)) {
            emitter.completeWithError(throwable);
        }
    }
}
