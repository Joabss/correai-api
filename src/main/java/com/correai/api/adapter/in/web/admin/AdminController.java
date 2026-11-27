package com.correai.api.adapter.in.web.admin;

import com.correai.api.domain.model.admin.AdminAccessDeniedException;
import com.correai.api.domain.port.in.admin.AdminUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private final AdminUseCase admins;
    private final byte[] bootstrapSecret;

    public AdminController(AdminUseCase admins,
                           @Value("${app.security.admin-bootstrap-secret:${ADMIN_BOOTSTRAP_SECRET:}}") String secret) {
        this.admins = admins;
        this.bootstrapSecret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public record RoleResponse(String role) {}

    @GetMapping("/role")
    public ResponseEntity<RoleResponse> role(@RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new RoleResponse(admins.isAdmin(userId) ? "ADMIN" : "USER"));
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<Void> bootstrap(@RequestAttribute("userId") UUID userId,
            @RequestHeader(value = "X-Admin-Bootstrap-Secret", required = false) String secret) {
        if (bootstrapSecret.length < 32 || secret == null
                || !MessageDigest.isEqual(bootstrapSecret, secret.getBytes(StandardCharsets.UTF_8))) {
            throw new AdminAccessDeniedException();
        }
        admins.bootstrap(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Void> grant(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        admins.grant(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> revoke(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        admins.revoke(userId, id);
        return ResponseEntity.noContent().build();
    }
}
