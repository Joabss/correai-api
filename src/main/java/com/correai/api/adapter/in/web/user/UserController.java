package com.correai.api.adapter.in.web.user;

import com.correai.api.adapter.in.web.auth.TokenResponse;
import com.correai.api.adapter.in.web.config.UserContextInterceptor;
import com.correai.api.domain.port.in.user.ConvertToAccountUseCase;
import com.correai.api.domain.port.in.user.CreateUserUseCase;
import com.correai.api.domain.port.in.user.GetUserUseCase;
import com.correai.api.domain.port.in.user.UpdateUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final ConvertToAccountUseCase convertToAccountUseCase;
    private final UpdateUserUseCase updateUserUseCase;

    public UserController(
            CreateUserUseCase createUserUseCase, GetUserUseCase getUserUseCase,
            ConvertToAccountUseCase convertToAccountUseCase, UpdateUserUseCase updateUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.getUserUseCase = getUserUseCase;
        this.convertToAccountUseCase = convertToAccountUseCase;
        this.updateUserUseCase = updateUserUseCase;
    }

    @Operation(summary = "Turns the authenticated anonymous (or name-only) user into a registered account")
    @PostMapping("/me/account")
    public UserResponse convertToAccount(
            @RequestAttribute(UserContextInterceptor.USER_REQUEST_ATTR) UUID userId,
            @Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(convertToAccountUseCase.convertToAccount(
                userId, request.name(), request.email(), request.password()));
    }

    @Operation(summary = "Registers a user with e-mail and password and returns a bearer token")
    @PostMapping
    public ResponseEntity<TokenResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TokenResponse.from(
                        createUserUseCase.create(request.name(), request.email(), request.password())));
    }

    @Operation(summary = "Returns the authenticated user")
    @GetMapping("/me")
    public UserResponse me(@RequestAttribute(UserContextInterceptor.USER_REQUEST_ATTR) UUID userId) {
        return UserResponse.from(getUserUseCase.getById(userId));
    }

    @PutMapping("/me")
    public UserResponse update(@RequestAttribute(UserContextInterceptor.USER_REQUEST_ATTR) UUID userId,
                               @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(updateUserUseCase.update(
                userId, request.name(), request.email(), request.currentPassword()));
    }
}
