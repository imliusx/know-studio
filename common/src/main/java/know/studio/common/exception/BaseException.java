package know.studio.common.exception;

import java.io.Serial;

/**
 * 自定义异常基类（三级异常体系顶层）。
 *
 * <p>层级：{@code RuntimeException → BaseException →} 具体异常
 * （{@link BusinessException} / {@link ForbiddenException} / {@link UnauthorizedException}）。
 * 每个异常都携带 {@link ErrorCode}，由 {@link GlobalExceptionHandler} 统一映射为 HTTP 响应。
 */
public abstract class BaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode, String message) {
        super(message != null && !message.isBlank() ? message : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    protected BaseException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
