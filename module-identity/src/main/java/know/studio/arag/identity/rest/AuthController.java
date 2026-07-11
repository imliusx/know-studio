package know.studio.arag.identity.rest;

import jakarta.validation.Valid;
import know.studio.arag.identity.api.CurrentIdentity;
import know.studio.arag.identity.domain.AuthSession;
import know.studio.arag.identity.domain.IdentityService;
import know.studio.arag.platform.core.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IdentityService identityService;

    @PostMapping("/register")
    public ApiResponse<AuthSession> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(identityService.register(request.email(), request.displayName(), request.password()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthSession> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(identityService.login(request.email(), request.password()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        identityService.logout();
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<CurrentIdentity> me() {
        return ApiResponse.ok(identityService.currentUser());
    }
}
