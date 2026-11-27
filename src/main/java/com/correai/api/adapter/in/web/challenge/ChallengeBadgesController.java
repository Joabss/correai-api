package com.correai.api.adapter.in.web.challenge;

import com.correai.api.adapter.in.web.dto.PageResponse;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.port.in.challenge.ChallengesUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/challenge-badges")
public class ChallengeBadgesController {
    private final ChallengesUseCase challenges;

    public ChallengeBadgesController(ChallengesUseCase challenges) {
        this.challenges = challenges;
    }

    @GetMapping("/me")
    public PageResponse<ChallengesUseCase.AwardedBadge> list(@RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PageQuery.DEFAULT_SIZE) int size) {
        return PageResponse.from(challenges.listBadges(userId, new PageQuery(page, size)));
    }
}
