package know.studio.common.exception;

import java.io.Serial;

/**
 * 未登录 / 登录失效异常，映射 {@link ErrorCode#UNAUTHORIZED}（401）。
 */
public class UnauthorizedException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDefaultMessage());
    }

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
