package com.correai.api.application.challenge;

import com.correai.api.domain.model.admin.AdminAccessDeniedException;
import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.model.challenge.ChallengeAudience;
import com.correai.api.domain.model.challenge.ChallengeNotFoundException;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import com.correai.api.domain.port.in.admin.AdminUseCase;
import com.correai.api.domain.port.in.badge.BadgeTemplatesUseCase;
import com.correai.api.domain.port.in.challenge.ChallengeDraftCommand;
import com.correai.api.domain.port.in.challenge.ChallengeDraftsUseCase;
import com.correai.api.domain.port.out.challenge.ChallengeRepositoryPort;
import com.correai.api.domain.port.out.admin.AdminRepositoryPort;
import com.correai.api.domain.port.out.group.GroupRepositoryPort;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class ChallengeDraftApplicationService implements ChallengeDraftsUseCase {
    private final ChallengeRepositoryPort challenges;
    private final GroupRepositoryPort groups;
    private final BadgeTemplatesUseCase badges;
    private final AdminUseCase admins;
    private final AdminRepositoryPort adminRepository;
    private final Clock clock;

    public ChallengeDraftApplicationService(ChallengeRepositoryPort challenges, GroupRepositoryPort groups,
                                            BadgeTemplatesUseCase badges, AdminUseCase admins,
                                            AdminRepositoryPort adminRepository, Clock clock) {
        this.challenges = challenges;
        this.groups = groups;
        this.badges = badges;
        this.admins = admins;
        this.adminRepository = adminRepository;
        this.clock = clock;
    }

    private void authorize(Challenge challenge) {
        if (challenge.audience() == ChallengeAudience.GLOBAL && !admins.isAdmin(challenge.creatorId())) {
            throw new AdminAccessDeniedException();
        }
        if (challenge.audience() == ChallengeAudience.GROUP
                && !groups.isMember(challenge.creatorId(), challenge.groupId())) {
            throw new ChallengeNotFoundException();
        }
        badges.get(challenge.badgeTemplateId());
    }

    @Override
    public Challenge create(UUID actor, ChallengeDraftCommand c) {
        Challenge challenge = new Challenge(UUID.randomUUID(), actor, c.name(), c.description(), c.audience(),
                c.groupId(), c.metric(), c.target(), c.startDate(), c.endDate(), c.timeZone(),
                c.badgeTemplateId(), clock.instant().truncatedTo(ChronoUnit.MICROS), null);
        authorize(challenge);
        return challenges.create(challenge);
    }

    @Override
    public Challenge edit(UUID actor, UUID id, ChallengeDraftCommand c) {
        Challenge original = getOwn(actor, id);
        Challenge challenge = original.revise(c.name(), c.description(), c.audience(), c.groupId(),
                c.metric(), c.target(), c.startDate(), c.endDate(), c.timeZone(), c.badgeTemplateId());
        authorize(challenge);
        return challenges.saveDraft(original, challenge);
    }

    @Override
    public Challenge getOwn(UUID actor, UUID id) {
        return challenges.findOwn(actor, id).orElseThrow(ChallengeNotFoundException::new);
    }

    @Override
    public Challenge publish(UUID actor, UUID id) {
        Challenge challenge = challenges.lockOwn(actor, id).orElseThrow(ChallengeNotFoundException::new);
        if (challenge.audience() == ChallengeAudience.GLOBAL && !adminRepository.isAdminLocked(actor)) {
            throw new AdminAccessDeniedException();
        }
        if (challenge.audience() == ChallengeAudience.GROUP
                && !groups.isMemberLocked(actor, challenge.groupId())) {
            throw new ChallengeNotFoundException();
        }
        badges.get(challenge.badgeTemplateId());
        return challenges.publish(challenge, clock.instant().truncatedTo(ChronoUnit.MICROS));
    }

    @Override
    public PageResult<Challenge> listOwn(UUID actor, PageQuery page) {
        return challenges.listOwn(actor, page);
    }
}
