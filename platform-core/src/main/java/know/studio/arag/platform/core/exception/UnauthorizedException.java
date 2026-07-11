package know.studio.arag.platform.core.exception;

/**
 * 未登录 / 登录失效异常，映射 {@link ErrorCode#UNAUTHORIZED}（401）。
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
