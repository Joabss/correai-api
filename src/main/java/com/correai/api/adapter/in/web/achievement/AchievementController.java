package com.correai.api.adapter.in.web.achievement;

import com.correai.api.adapter.in.web.achievement.dto.AchievementResponse;
import com.correai.api.domain.port.in.achievement.AchievementsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/achievements")
@SecurityRequirement(name = "bearerAuth")
public class AchievementController {

    private final AchievementsUseCase achievementsUseCase;

    public AchievementController(AchievementsUseCase achievementsUseCase) {
        this.achievementsUseCase = achievementsUseCase;
    }

    @Operation(summary = "Lists the full achievement badge catalog with unlocked state for the user")
    @GetMapping("/me")
    @Transactional
    public ResponseEntity<List<AchievementResponse>> list(@RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(achievementsUseCase.list(userId).stream().map(AchievementResponse::from).toList());
    }
}
