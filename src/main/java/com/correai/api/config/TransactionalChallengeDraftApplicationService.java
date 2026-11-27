package com.correai.api.config;

import com.correai.api.application.challenge.ChallengeDraftApplicationService;
import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.port.in.admin.AdminUseCase;
import com.correai.api.domain.port.in.badge.BadgeTemplatesUseCase;
import com.correai.api.domain.port.out.admin.AdminRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeRepositoryPort;
import com.correai.api.domain.port.out.group.GroupRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

public class TransactionalChallengeDraftApplicationService extends ChallengeDraftApplicationService {
    public TransactionalChallengeDraftApplicationService(ChallengeRepositoryPort challenges, GroupRepositoryPort groups,
            BadgeTemplatesUseCase badges, AdminUseCase admins, AdminRepositoryPort adminRepository, Clock clock) {
        super(challenges, groups, badges, admins, adminRepository, clock);
    }

    @Override
    @Transactional
    public Challenge publish(UUID actor, UUID id) {
        return super.publish(actor, id);
    }
}
