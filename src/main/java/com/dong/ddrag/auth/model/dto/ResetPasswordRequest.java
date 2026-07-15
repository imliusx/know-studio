package com.dong.ddrag.auth.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "用户名不能为空")
        String username,
        @NotBlank(message = "邮箱不能为空")
        String email,
        @NotBlank(message = "新密码不能为空")
        String newPassword
) {
}
