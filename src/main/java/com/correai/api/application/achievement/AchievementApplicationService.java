package com.correai.api.application.achievement;

import com.correai.api.application.media.GpxReader;
import com.correai.api.domain.model.achievement.Achievement;
import com.correai.api.domain.model.achievement.BadgeCode;
import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.BestEffortCalculator;
import com.correai.api.domain.model.activity.TimedRoutePoint;
import com.correai.api.domain.model.goal.Goal;
import com.correai.api.domain.model.goal.GoalPeriod;
import com.correai.api.domain.model.goal.GoalType;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.port.in.achievement.AchievementsUseCase;
import com.correai.api.domain.port.in.stats.GetStatsSummaryUseCase;
import com.correai.api.domain.port.in.stats.PersonalRecordsUseCase;
import com.correai.api.domain.port.in.stats.StatsSummary;
import com.correai.api.domain.port.out.achievement.AchievementRepositoryPort;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.goal.GoalRepositoryPort;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Evaluates and persists achievement badges unlocked by a user's activity, goal and route history.
 * Decoupled from the Goal/challenge "badge" system: achievements never revert once unlocked.
 */
public class AchievementApplicationService implements AchievementsUseCase {

    private static final Logger log = LoggerFactory.getLogger(AchievementApplicationService.class);

    /** Heuristic radius (meters) beyond which a route's start point counts as "a new place". */
    private static final double EXPLORER_RADIUS_METERS = 2000;
    private static final LocalTime MADRUGADOR_BEFORE = LocalTime.of(6, 0);
    private static final LocalTime CORUJA_AT_OR_AFTER = LocalTime.of(21, 0);
    private static final LocalTime SOL_A_PINO_START = LocalTime.of(11, 0);
    private static final LocalTime SOL_A_PINO_END = LocalTime.of(15, 0);
    private static final int CORACAO_FORTE_CONSECUTIVE_WEEKS = 4;

    private final ActivityRepositoryPort activities;
    private final GetStatsSummaryUseCase stats;
    private final PersonalRecordsUseCase personalRecords;
    private final GoalRepositoryPort goals;
    private final MediaRepositoryPort media;
    private final AchievementRepositoryPort repository;
    private final Clock clock;

    public AchievementApplicationService(ActivityRepositoryPort activities, GetStatsSummaryUseCase stats,
                                          PersonalRecordsUseCase personalRecords, GoalRepositoryPort goals,
                                          MediaRepositoryPort media, AchievementRepositoryPort repository,
                                          Clock clock) {
        this.activities = activities;
        this.stats = stats;
        this.personalRecords = personalRecords;
        this.goals = goals;
        this.media = media;
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public List<Achievement> list(UUID userId) {
        evaluate(userId);
        Map<BadgeCode, Instant> unlocked = repository.findUnlockedByUserId(userId);
        List<Achievement> result = new ArrayList<>();
        for (BadgeCode code : BadgeCode.values()) {
            Instant unlockedAt = unlocked.get(code);
            result.add(unlockedAt != null ? Achievement.unlocked(code, unlockedAt) : Achievement.locked(code));
        }
        return result;
    }

    @Override
    public List<BadgeCode> evaluate(UUID userId) {
        Map<BadgeCode, Instant> unlocked = repository.findUnlockedByUserId(userId);
        Map<BadgeCode, Instant> satisfied = satisfiedCodes(userId);
        List<BadgeCode> granted = new ArrayList<>();
        for (Map.Entry<BadgeCode, Instant> entry : satisfied.entrySet()) {
            BadgeCode code = entry.getKey();
            Instant attributed = entry.getValue();
            Instant existing = unlocked.get(code);
            if (existing == null) {
                if (repository.grantIfAbsent(userId, code, attributed)) {
                    granted.add(code);
                }
            } else if (attributed.isBefore(existing)) {
                // Fixes badges unlocked before the actual qualifying date was attributable (e.g. a
                // badge evaluated the day after a backdated manual activity was entered).
                repository.correctUnlockedAt(userId, code, attributed);
            }
        }
        return granted;
    }

    /**
     * Returns the codes currently satisfied by the user's history, each mapped to the moment it was
     * first achieved. Whenever a badge can be attributed to a specific activity, its own date/time is
     * used instead of the evaluation moment, so a badge earned by a backdated manual entry keeps that
     * entry's date rather than the date it happened to be evaluated.
     */
    private Map<BadgeCode, Instant> satisfiedCodes(UUID userId) {
        Map<BadgeCode, Instant> result = new EnumMap<>(BadgeCode.class);
        Instant now = Instant.now(clock);
        List<Activity> all = activities.findAllByUserId(userId);

        StatsSummary summary = stats.getSummary(userId);
        if (summary.longestDistance() > 0) {
            result.put(BadgeCode.PRIMEIRO_PASSO, earliestQualifying(all, a -> true).orElse(now));
        }
        if (summary.longestDistance() >= 5.0) {
            result.put(BadgeCode.CORRIDA_5K, earliestQualifying(all, distanceAtLeast(5.0)).orElse(now));
        }
        if (summary.longestDistance() >= 10.0) {
            result.put(BadgeCode.CORRIDA_10K, earliestQualifying(all, distanceAtLeast(10.0)).orElse(now));
        }
        if (summary.longestDistance() >= 21.0975) {
            result.put(BadgeCode.MEIA_MARATONA, earliestQualifying(all, distanceAtLeast(21.0975)).orElse(now));
        }
        if (summary.longestDistance() >= 42.195) {
            result.put(BadgeCode.MARATONA, earliestQualifying(all, distanceAtLeast(42.195)).orElse(now));
        }
        if (summary.streak() >= 7) result.put(BadgeCode.SEQUENCIA_7, now);
        if (summary.streak() >= 30) result.put(BadgeCode.SEQUENCIA_30, now);
        if (summary.kmMonth() >= 100) result.put(BadgeCode.CENTURIAO, now);

        Predicate<Activity> isMadrugador = a -> a.startTime() != null && a.startTime().isBefore(MADRUGADOR_BEFORE);
        if (all.stream().anyMatch(isMadrugador)) {
            result.put(BadgeCode.MADRUGADOR, earliestQualifying(all, isMadrugador).orElse(now));
        }
        Predicate<Activity> isCoruja = a -> a.startTime() != null && !a.startTime().isBefore(CORUJA_AT_OR_AFTER);
        if (all.stream().anyMatch(isCoruja)) {
            result.put(BadgeCode.CORUJA, earliestQualifying(all, isCoruja).orElse(now));
        }
        Predicate<Activity> isSolAPino = a -> a.startTime() != null
                && !a.startTime().isBefore(SOL_A_PINO_START) && !a.startTime().isAfter(SOL_A_PINO_END);
        if (all.stream().anyMatch(isSolAPino)) {
            result.put(BadgeCode.SOL_A_PINO, earliestQualifying(all, isSolAPino).orElse(now));
        }

        List<UUID> recordActivityIds = personalRecords.records(userId).stream()
                .flatMap(r -> r.efforts().stream())
                .map(PersonalRecordsUseCase.Effort::activityId)
                .toList();
        if (!recordActivityIds.isEmpty()) {
            Instant recordInstant = earliestQualifying(all, a -> recordActivityIds.contains(a.id())).orElse(now);
            result.put(BadgeCode.RECORDE_PESSOAL, recordInstant);
        }

        List<Goal> awarded = goals.findAwardedByUserId(userId);
        awarded.stream()
                .filter(g -> g.type() == GoalType.AVG_PACE)
                .map(Goal::achievedAt)
                .min(Comparator.naturalOrder())
                .ifPresent(instant -> result.put(BadgeCode.RELAMPAGO, instant));
        consecutiveWeeklyStreakCompletedAt(awarded, CORACAO_FORTE_CONSECUTIVE_WEEKS)
                .ifPresent(instant -> result.put(BadgeCode.CORACAO_FORTE, instant));

        exploredNewRouteAt(userId, all).ifPresent(instant -> result.put(BadgeCode.EXPLORADOR, instant));

        return result;
    }

    private static Predicate<Activity> distanceAtLeast(double km) {
        return a -> a.distanceKm() != null && a.distanceKm() >= km;
    }

    /** Earliest activity (by its own date/time) matching the predicate, if any. */
    private Instant activityInstant(Activity activity) {
        if (activity.startedAt() != null) return activity.startedAt();
        return activity.activityDate().atTime(LocalTime.NOON).atZone(ZoneOffset.UTC).toInstant();
    }

    private Optional<Instant> earliestQualifying(List<Activity> all, Predicate<Activity> predicate) {
        return all.stream()
                .filter(predicate)
                .map(this::activityInstant)
                .min(Comparator.naturalOrder());
    }

    /**
     * If there are {@code weeks} achieved weekly ACTIVITIES goals on consecutive weeks, returns the
     * {@code achievedAt} of the goal that completed the streak.
     */
    private Optional<Instant> consecutiveWeeklyStreakCompletedAt(List<Goal> awarded, int weeks) {
        List<Goal> weekly = awarded.stream()
                .filter(g -> g.type() == GoalType.ACTIVITIES && g.period() == GoalPeriod.WEEKLY)
                .collect(Collectors.toMap(Goal::achievedPeriodStart, g -> g, (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparing(Goal::achievedPeriodStart))
                .toList();
        int run = weekly.isEmpty() ? 0 : 1;
        for (int i = 1; i < weekly.size(); i++) {
            LocalDate previousStart = weekly.get(i - 1).achievedPeriodStart();
            run = weekly.get(i).achievedPeriodStart().equals(previousStart.plusWeeks(1)) ? run + 1 : 1;
            if (run >= weeks) return Optional.of(weekly.get(i).achievedAt());
        }
        return run >= weeks ? Optional.of(weekly.get(weekly.size() - 1).achievedAt()) : Optional.empty();
    }

    /**
     * Heuristic: unlocked when the user has recorded GPX routes starting in at least two distinct
     * places, at least {@link #EXPLORER_RADIUS_METERS} apart. Attributed to the activity whose route
     * start first proved to be a new place.
     */
    private Optional<Instant> exploredNewRouteAt(UUID userId, List<Activity> all) {
        List<TimedRoutePoint> knownStarts = new ArrayList<>();
        for (UUID activityId : media.listRouteActivityIds(userId)) {
            var content = media.find(userId, activityId, MediaKind.ACTIVITY_ROUTE);
            if (content.isEmpty()) continue;
            List<List<TimedRoutePoint>> segments;
            try {
                segments = GpxReader.read(content.get().bytes());
            } catch (IllegalArgumentException e) {
                log.warn("Ignorando rota inválida da atividade {} ao avaliar conquistas: {}",
                        activityId, e.getMessage());
                continue;
            }
            if (segments.isEmpty() || segments.getFirst().isEmpty()) continue;
            TimedRoutePoint start = segments.getFirst().getFirst();
            boolean isNewPlace = knownStarts.stream()
                    .noneMatch(known -> BestEffortCalculator.distanceMeters(known, start) < EXPLORER_RADIUS_METERS);
            if (isNewPlace && !knownStarts.isEmpty()) {
                Instant attributed = all.stream()
                        .filter(a -> a.id().equals(activityId))
                        .findFirst()
                        .map(this::activityInstant)
                        .orElseGet(() -> Instant.now(clock));
                return Optional.of(attributed);
            }
            knownStarts.add(start);
        }
        return Optional.empty();
    }
}

