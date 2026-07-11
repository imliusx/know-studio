package know.studio.arag.platform.core.exception;

import org.springframework.http.HttpStatus;

/**
 * 统一错误码。code 命名：A 段=客户端错误，B 段=服务端错误（参考阿里规约风格）。
 */
public enum ErrorCode {

    BAD_REQUEST("A0400", "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("A0401", "未登录或登录已失效", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("A0403", "无权限访问", HttpStatus.FORBIDDEN),
    NOT_FOUND("A0404", "资源不存在", HttpStatus.NOT_FOUND),
    CONFLICT("A0409", "资源冲突", HttpStatus.CONFLICT),
    BUSINESS_ERROR("A0500", "业务处理失败", HttpStatus.BAD_REQUEST),
    SYSTEM_ERROR("B0500", "系统内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
