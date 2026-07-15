package com.dong.ddrag.auth.controller;

import com.dong.ddrag.auth.model.dto.LoginRequest;
import com.dong.ddrag.auth.model.dto.RegisterRequest;
import com.dong.ddrag.auth.model.dto.ResetPasswordRequest;
import com.dong.ddrag.auth.model.vo.AuthTokensResponse;
import com.dong.ddrag.auth.model.vo.CurrentUserProfileResponse;
import com.dong.ddrag.auth.service.AuthService;
import com.dong.ddrag.auth.service.AuthService.AuthTokens;
import com.dong.ddrag.common.api.ApiResponse;
import com.dong.ddrag.identity.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(
            AuthService authService,
            CurrentUserService currentUserService
    ) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request.loginId(), request.password());
        return ApiResponse.success(AuthTokensResponse.from(tokens, authService.getCurrentUser(tokens.userId())));
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPasswordByIdentity(request.username(), request.email(), request.newPassword());
        return ApiResponse.success(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserProfileResponse> currentUser(HttpServletRequest request) {
        return ApiResponse.success(CurrentUserProfileResponse.from(currentUserService.getRequiredCurrentUser(request)));
    }
}
