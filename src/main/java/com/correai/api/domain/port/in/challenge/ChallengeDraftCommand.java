package com.correai.api.domain.port.in.challenge;

import com.correai.api.domain.model.challenge.ChallengeAudience;
import com.correai.api.domain.model.challenge.ChallengeMetric;
import java.time.LocalDate;
import java.util.UUID;

public record ChallengeDraftCommand(String name, String description, ChallengeAudience audience, UUID groupId,
                                    ChallengeMetric metric, double target, LocalDate startDate, LocalDate endDate,
                                    String timeZone, UUID badgeTemplateId) {}
