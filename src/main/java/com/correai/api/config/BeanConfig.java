package com.correai.api.config;

import com.correai.api.application.challenge.ChallengeActivityEvaluationService;
import com.correai.api.application.achievement.AchievementApplicationService;
import com.correai.api.application.common.StreakCalculator;
import com.correai.api.application.goal.GoalApplicationService;
import com.correai.api.application.group.GroupApplicationService;
import com.correai.api.application.badge.BadgeTemplateApplicationService;
import com.correai.api.domain.port.out.challenge.ChallengeRepositoryPort;
import com.correai.api.domain.port.out.challenge.ChallengeParticipationRepositoryPort;
import com.correai.api.domain.port.in.badge.BadgeTemplatesUseCase;
import com.correai.api.domain.port.out.badge.BadgeTemplateRepositoryPort;
import com.correai.api.domain.port.in.admin.AdminUseCase;
import com.correai.api.application.admin.AdminApplicationService;
import com.correai.api.domain.port.out.admin.AdminRepositoryPort;
import com.correai.api.application.friendship.FriendshipApplicationService;
import com.correai.api.domain.port.out.friendship.FriendshipRepositoryPort;
import com.correai.api.domain.port.out.group.GroupRepositoryPort;
import com.correai.api.application.stats.StatsApplicationService;
import com.correai.api.application.stats.PersonalRecordApplicationService;
import com.correai.api.application.auth.AuthApplicationService;
import com.correai.api.application.user.UserApplicationService;
import com.correai.api.application.media.MediaApplicationService;
import com.correai.api.domain.port.out.achievement.AchievementRepositoryPort;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.auth.OneTimeTokenPort;
import com.correai.api.domain.port.out.auth.PasswordHasherPort;
import com.correai.api.domain.port.out.auth.PasswordResetNotifierPort;
import com.correai.api.domain.port.out.auth.TokenPort;
import com.correai.api.domain.port.out.goal.GoalRepositoryPort;
import com.correai.api.domain.port.out.user.UserRepositoryPort;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;
import com.correai.api.domain.port.in.goal.EvaluateGoalsUseCase;
import com.correai.api.domain.port.in.achievement.AchievementsUseCase;
import com.correai.api.domain.port.in.stats.GetStatsSummaryUseCase;
import com.correai.api.domain.port.in.stats.PersonalRecordsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class BeanConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    @Bean
    public StreakCalculator streakCalculator(ActivityRepositoryPort repository, Clock clock) {
        return new StreakCalculator(repository, clock);
    }

    @Bean
    public TransactionalActivityApplicationService activityApplicationService(
            ActivityRepositoryPort repository, StreakCalculator streakCalculator, Clock clock,
            EvaluateGoalsUseCase goals, AchievementsUseCase achievements,
            ChallengeActivityEvaluationService challengeEvaluation) {
        return new TransactionalActivityApplicationService(repository, streakCalculator, clock, goals, achievements,
                challengeEvaluation);
    }

    @Bean
    public ChallengeActivityEvaluationService challengeActivityEvaluationService(
            ChallengeParticipationRepositoryPort participations, ChallengeRepositoryPort challenges,
            ActivityRepositoryPort activities, Clock clock) {
        return new ChallengeActivityEvaluationService(participations, challenges, activities, clock);
    }

    @Bean
    public TransactionalChallengesApplicationService challengesApplicationService(
            ChallengeRepositoryPort challenges, ChallengeParticipationRepositoryPort participations,
            FriendshipRepositoryPort friendships, GroupRepositoryPort groups, ActivityRepositoryPort activities,
            ChallengeActivityEvaluationService evaluation, BadgeTemplatesUseCase badges, Clock clock) {
        return new TransactionalChallengesApplicationService(challenges, participations, friendships, groups,
                activities, evaluation, badges, clock);
    }

    @Bean
    public StatsApplicationService statsApplicationService(
            ActivityRepositoryPort repository, StreakCalculator streakCalculator, Clock clock) {
        return new StatsApplicationService(repository, streakCalculator, clock);
    }

    @Bean
    public PersonalRecordApplicationService personalRecordApplicationService(MediaRepositoryPort media) {
        return new PersonalRecordApplicationService(media);
    }

    @Bean
    public AchievementApplicationService achievementApplicationService(
            ActivityRepositoryPort activityRepository, GetStatsSummaryUseCase stats,
            PersonalRecordsUseCase personalRecords, GoalRepositoryPort goalRepository,
            MediaRepositoryPort media, AchievementRepositoryPort achievementRepository, Clock clock) {
        return new AchievementApplicationService(activityRepository, stats, personalRecords, goalRepository,
                media, achievementRepository, clock);
    }

    @Bean
    public GoalApplicationService goalApplicationService(
            GoalRepositoryPort goalRepository, ActivityRepositoryPort activityRepository, Clock clock) {
        return new GoalApplicationService(goalRepository, activityRepository, clock);
    }

    @Bean
    public GroupApplicationService groupApplicationService(GroupRepositoryPort groups, Clock clock) {
        return new GroupApplicationService(groups, clock);
    }

    @Bean
    public AdminApplicationService adminApplicationService(AdminRepositoryPort admins, Clock clock) {
        return new AdminApplicationService(admins, clock);
    }

    @Bean
    public BadgeTemplateApplicationService badgeTemplateApplicationService(
            BadgeTemplateRepositoryPort templates, AdminUseCase admins, Clock clock) {
        return new BadgeTemplateApplicationService(templates, admins, clock);
    }

    @Bean
    public TransactionalChallengeDraftApplicationService challengeDraftApplicationService(
            ChallengeRepositoryPort challenges, GroupRepositoryPort groups, BadgeTemplatesUseCase badges,
            AdminUseCase admins, AdminRepositoryPort adminRepository, Clock clock) {
        return new TransactionalChallengeDraftApplicationService(challenges, groups, badges, admins, adminRepository,
                clock);
    }

    @Bean
    public FriendshipApplicationService friendshipApplicationService(FriendshipRepositoryPort friendships, Clock clock) {
        return new FriendshipApplicationService(friendships, clock);
    }

    @Bean
    public AuthApplicationService authApplicationService(
            UserRepositoryPort repository, TokenPort tokenPort, PasswordHasherPort passwordHasher,
            OneTimeTokenPort oneTimeTokens, PasswordResetNotifierPort resetNotifier) {
        return new AuthApplicationService(repository, tokenPort, passwordHasher, oneTimeTokens, resetNotifier);
    }

    @Bean
    public UserApplicationService userApplicationService(
            UserRepositoryPort repository, TokenPort tokenPort, PasswordHasherPort passwordHasher,
            OneTimeTokenPort oneTimeTokens) {
        return new UserApplicationService(repository, tokenPort, passwordHasher, oneTimeTokens);
    }

    @Bean
    public MediaApplicationService mediaApplicationService(
            MediaRepositoryPort media, ActivityRepositoryPort activities) {
        return new MediaApplicationService(media, activities);
    }
}
