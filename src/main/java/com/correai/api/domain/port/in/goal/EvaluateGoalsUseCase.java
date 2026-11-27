package com.correai.api.domain.port.in.goal;

import java.util.UUID;
import java.util.List;

public interface EvaluateGoalsUseCase {
    List<UUID> evaluate(UUID userId);
}
