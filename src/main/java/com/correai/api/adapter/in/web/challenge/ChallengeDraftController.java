package com.correai.api.adapter.in.web.challenge;

import com.correai.api.adapter.in.web.dto.PageResponse;
import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.model.challenge.ChallengeAudience;
import com.correai.api.domain.model.challenge.ChallengeMetric;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.port.in.challenge.ChallengeDraftCommand;
import com.correai.api.domain.port.in.challenge.ChallengeDraftsUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/challenge-drafts")
@SecurityRequirement(name = "bearerAuth")
public class ChallengeDraftController {
    private final ChallengeDraftsUseCase drafts;

    public ChallengeDraftController(ChallengeDraftsUseCase drafts) {
        this.drafts = drafts;
    }

    public record DraftRequest(@NotBlank @Size(max = 100) String name,
                               @NotBlank @Size(max = 500) String description,
                               @NotNull ChallengeAudience audience, UUID groupId,
                               @NotNull ChallengeMetric metric, @NotNull @Positive Double target,
                               @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                               @NotBlank String timeZone, @NotNull UUID badgeTemplateId) {
        ChallengeDraftCommand command() {
            return new ChallengeDraftCommand(name, description, audience, groupId, metric, target,
                    startDate, endDate, timeZone, badgeTemplateId);
        }
    }

    @PostMapping
    public ResponseEntity<Challenge> create(@RequestAttribute("userId") UUID userId,
                                             @Valid @RequestBody DraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(drafts.create(userId, request.command()));
    }

    @PutMapping("/{id}")
    public Challenge edit(@RequestAttribute("userId") UUID userId, @PathVariable UUID id,
                          @Valid @RequestBody DraftRequest request) {
        return drafts.edit(userId, id, request.command());
    }

    @PostMapping("/{id}/publish")
    public Challenge publish(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        return drafts.publish(userId, id);
    }

    @GetMapping("/{id}")
    public Challenge get(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        return drafts.getOwn(userId, id);
    }

    @GetMapping
    public PageResponse<Challenge> list(@RequestAttribute("userId") UUID userId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "" + PageQuery.DEFAULT_SIZE) int size) {
        return PageResponse.from(drafts.listOwn(userId, new PageQuery(page, size)));
    }
}
