package com.correai.api.adapter.in.web.goal;

import com.correai.api.domain.model.goal.Goal;
import com.correai.api.domain.port.in.goal.CreateGoalCommand;
import com.correai.api.domain.port.in.goal.CreateGoalUseCase;
import com.correai.api.domain.port.in.goal.DeactivateGoalUseCase;
import com.correai.api.domain.port.in.goal.ListGoalsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/goals")
@SecurityRequirement(name = "bearerAuth")
public class GoalController {

    private final CreateGoalUseCase createGoalUseCase;
    private final ListGoalsUseCase listGoalsUseCase;
    private final DeactivateGoalUseCase deactivateGoalUseCase;

    public GoalController(CreateGoalUseCase createGoalUseCase,
                          ListGoalsUseCase listGoalsUseCase,
                          DeactivateGoalUseCase deactivateGoalUseCase) {
        this.createGoalUseCase = createGoalUseCase;
        this.listGoalsUseCase = listGoalsUseCase;
        this.deactivateGoalUseCase = deactivateGoalUseCase;
    }

    @Operation(summary = "Creates a goal, replacing the active goal of the same type and period")
    @PostMapping
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
    public ResponseEntity<List<GoalResponse>> list(@RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(listGoalsUseCase.listActive(userId).stream().map(GoalResponse::from).toList());
    }

    @Operation(summary = "Deactivates a goal")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        deactivateGoalUseCase.deactivate(userId, id);
        return ResponseEntity.noContent().build();
    }
}
