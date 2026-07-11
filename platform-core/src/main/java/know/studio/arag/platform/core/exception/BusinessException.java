package know.studio.arag.platform.core.exception;

import java.io.Serial;

/**
 * 业务异常。默认映射 {@link ErrorCode#BUSINESS_ERROR}（400），也可显式指定其他错误码。
 */
public class BusinessException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(ErrorCode.BUSINESS_ERROR, message);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
