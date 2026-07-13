package know.studio.common.exception;

import java.io.Serial;

/**
 * 无权限访问异常，映射 {@link ErrorCode#FORBIDDEN}（403）。
 */
public class ForbiddenException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getDefaultMessage());
    }

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
