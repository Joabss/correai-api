package com.correai.api.adapter.in.web.auth;

import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final IssueAnonymousTokenUseCase issueAnonymousTokenUseCase;

    public AuthController(IssueAnonymousTokenUseCase issueAnonymousTokenUseCase) {
        this.issueAnonymousTokenUseCase = issueAnonymousTokenUseCase;
    }

    @Operation(summary = "Creates an anonymous user and returns a bearer token")
    @PostMapping("/anonymous")
    public ResponseEntity<TokenResponse> anonymous() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TokenResponse.from(issueAnonymousTokenUseCase.issue()));
    }
}
