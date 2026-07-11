package know.studio.arag.platform.core.exception;

/**
 * 无权限访问异常，映射 {@link ErrorCode#FORBIDDEN}（403）。
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
