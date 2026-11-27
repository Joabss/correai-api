package com.correai.api.adapter.in.web.goal;

import com.correai.api.domain.model.goal.Goal;
import com.correai.api.domain.model.goal.GoalHasBadgeException;
import com.correai.api.domain.port.in.goal.CreateGoalCommand;
import com.correai.api.domain.port.in.goal.CreateGoalUseCase;
import com.correai.api.domain.port.in.goal.DeactivateGoalUseCase;
import com.correai.api.domain.port.in.goal.ListGoalsUseCase;
import com.correai.api.domain.port.in.goal.GoalBadgesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/goals")
@SecurityRequirement(name = "bearerAuth")
public class GoalController {

    private final CreateGoalUseCase createGoalUseCase;
    private final ListGoalsUseCase listGoalsUseCase;
    private final DeactivateGoalUseCase deactivateGoalUseCase;
    private final GoalBadgesUseCase goalBadgesUseCase;

    public GoalController(CreateGoalUseCase createGoalUseCase,
                          ListGoalsUseCase listGoalsUseCase,
                          DeactivateGoalUseCase deactivateGoalUseCase,
                          GoalBadgesUseCase goalBadgesUseCase) {
        this.createGoalUseCase = createGoalUseCase;
        this.listGoalsUseCase = listGoalsUseCase;
        this.deactivateGoalUseCase = deactivateGoalUseCase;
        this.goalBadgesUseCase = goalBadgesUseCase;
    }

    @Operation(summary = "Creates a challenge, replacing only unawarded challenges of the same type and period")
    @PostMapping
    @Transactional
    public ResponseEntity<GoalResponse> create(
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody GoalRequest request
    ) {
        Goal goal = createGoalUseCase.create(
                new CreateGoalCommand(userId, request.type(), request.period(), request.target()));
        return ResponseEntity.status(HttpStatus.CREATED).body(GoalResponse.from(goal));
    }

    @Operation(summary = "Lists the active goals with their progress in the current period")
    @GetMapping
    @Transactional
    public ResponseEntity<List<GoalResponse>> list(@RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(listGoalsUseCase.listActive(userId).stream().map(GoalResponse::from).toList());
    }

    @GetMapping("/badges")
    @Transactional
    public List<GoalResponse> badges(@RequestAttribute("userId") UUID userId) {
        return goalBadgesUseCase.listBadges(userId).stream().map(GoalResponse::from).toList();
    }

    @GetMapping(value = "/{id}/badge", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> badge(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(BadgeImage.render(goalBadgesUseCase.getBadge(userId, id)));
    }

    @Operation(summary = "Deactivates a goal")
    @DeleteMapping("/{id}")
    @Transactional(noRollbackFor = GoalHasBadgeException.class)
    public ResponseEntity<Void> deactivate(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        deactivateGoalUseCase.deactivate(userId, id);
        return ResponseEntity.noContent().build();
    }
}
