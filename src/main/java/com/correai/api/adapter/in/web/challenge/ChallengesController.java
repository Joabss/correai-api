package com.correai.api.adapter.in.web.challenge;

import com.correai.api.adapter.in.web.dto.PageResponse;
import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.port.in.challenge.ChallengesUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/challenges")
public class ChallengesController {
    private final ChallengesUseCase challenges;

    public ChallengesController(ChallengesUseCase challenges) {
        this.challenges = challenges;
    }

    @GetMapping
    public PageResponse<Challenge> list(@RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PageQuery.DEFAULT_SIZE) int size) {
        return PageResponse.from(challenges.list(userId, new PageQuery(page, size)));
    }

    @GetMapping("/{id}")
    public ChallengesUseCase.Details get(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        return challenges.get(userId, id);
    }

    @PutMapping("/{id}/participation")
    public ChallengesUseCase.Details join(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        return challenges.join(userId, id);
    }
}
