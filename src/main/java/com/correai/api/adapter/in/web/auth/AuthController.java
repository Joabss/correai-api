package com.correai.api.adapter.in.web.auth;

import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import com.correai.api.domain.port.in.auth.LoginUseCase;
import com.correai.api.domain.port.in.auth.PasswordResetUseCase;
import com.correai.api.domain.port.in.auth.RefreshTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final IssueAnonymousTokenUseCase issueAnonymousTokenUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final PasswordResetUseCase passwordResetUseCase;

    public AuthController(
            IssueAnonymousTokenUseCase issueAnonymousTokenUseCase, LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase, PasswordResetUseCase passwordResetUseCase) {
        this.issueAnonymousTokenUseCase = issueAnonymousTokenUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.passwordResetUseCase = passwordResetUseCase;
    }

    @Operation(summary = "Exchanges a refresh token for a new access token and a rotated refresh token")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return TokenResponse.from(refreshTokenUseCase.refresh(request.refreshToken()));
    }

    @Operation(summary = "Requests a password reset; always answers 202 regardless of the e-mail existing")
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetUseCase.request(request.email());
    }

    @Operation(summary = "Sets a new password using a reset token")
    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetUseCase.reset(request.token(), request.newPassword());
    }

    @Operation(summary = "Creates an anonymous user and returns a bearer token")
    @PostMapping("/anonymous")
    public ResponseEntity<TokenResponse> anonymous() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TokenResponse.from(issueAnonymousTokenUseCase.issue()));
    }

    @Operation(summary = "Authenticates an existing user by e-mail and password and returns a bearer token")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(loginUseCase.login(request.email(), request.password()));
    }
}
