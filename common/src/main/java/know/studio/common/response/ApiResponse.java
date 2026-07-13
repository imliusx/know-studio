package know.studio.common.response;

/**
 * 统一响应结构。所有 REST 接口返回 {@code ApiResponse<T>}。
 *
 * @param success 是否成功
 * @param code    业务码（成功为 "0"，失败为 ErrorCode 的 code）
 * @param data    业务数据（失败为 null）
 * @param message 失败消息（成功为 null）
 */
public record ApiResponse<T>(boolean success, String code, T data, String message) {

    private static final String OK_CODE = "0";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, OK_CODE, data, null);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, OK_CODE, null, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, code, null, message);
    }
}
