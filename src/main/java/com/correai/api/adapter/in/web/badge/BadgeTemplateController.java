package com.correai.api.adapter.in.web.badge;

import com.correai.api.adapter.in.web.dto.PageResponse;
import com.correai.api.domain.model.badge.BadgeIcon;
import com.correai.api.domain.model.badge.BadgeTemplate;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.port.in.badge.BadgeTemplatesUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/badge-templates")
@SecurityRequirement(name = "bearerAuth")
public class BadgeTemplateController {
    private final BadgeTemplatesUseCase templates;

    public BadgeTemplateController(BadgeTemplatesUseCase templates) {
        this.templates = templates;
    }

    public record CreateBadgeTemplateRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Size(max = 300) String description,
            @NotNull BadgeIcon icon,
            @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color) {}

    @PostMapping
    public ResponseEntity<BadgeTemplate> create(@RequestAttribute("userId") UUID userId,
            @Valid @RequestBody CreateBadgeTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templates.create(userId, request.name(),
                request.description(), request.icon(), request.color()));
    }

    @GetMapping
    public PageResponse<BadgeTemplate> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "" + PageQuery.DEFAULT_SIZE) int size) {
        return PageResponse.from(templates.list(new PageQuery(page, size)));
    }

    @GetMapping("/{id}")
    public BadgeTemplate get(@PathVariable UUID id) {
        return templates.get(id);
    }

    @GetMapping(value = "/{id}/image", produces = "image/svg+xml")
    public ResponseEntity<String> image(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("image/svg+xml"))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(BadgeTemplateImage.render(templates.get(id)));
    }
}
