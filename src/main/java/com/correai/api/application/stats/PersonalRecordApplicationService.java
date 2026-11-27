package com.correai.api.application.stats;

import com.correai.api.application.media.GpxReader;
import com.correai.api.domain.model.activity.BestEffortCalculator;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.port.in.stats.PersonalRecordsUseCase;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class PersonalRecordApplicationService implements PersonalRecordsUseCase {
    private static final List<Double> DISTANCES_KM = List.of(5.0, 10.0, 21.0975, 42.195);
    private final MediaRepositoryPort media;

    public PersonalRecordApplicationService(MediaRepositoryPort media) {
        this.media = media;
    }

    @Override
    public List<PersonalRecord> records(UUID userId) {
        List<List<Effort>> rankings = DISTANCES_KM.stream().<List<Effort>>map(_ -> new ArrayList<>()).toList();
        for (UUID activityId : media.listRouteActivityIds(userId)) {
            var content = media.find(userId, activityId, MediaKind.ACTIVITY_ROUTE);
            // An activity may have been deleted after the ID projection was read.
            if (content.isEmpty()) continue;
            var segments = GpxReader.read(content.get().bytes());
            for (int i = 0; i < DISTANCES_KM.size(); i++) {
                double distance = DISTANCES_KM.get(i) * 1000;
                var fastest = segments.stream()
                        .map(segment -> BestEffortCalculator.fastestSeconds(segment, distance))
                        .filter(java.util.OptionalDouble::isPresent)
                        .mapToDouble(java.util.OptionalDouble::getAsDouble).min();
                if (fastest.isPresent()) {
                    List<Effort> ranking = rankings.get(i);
                    ranking.add(new Effort(activityId, fastest.getAsDouble()));
                    ranking.sort(Comparator.comparingDouble(Effort::durationSeconds).thenComparing(Effort::activityId));
                    if (ranking.size() > 3) ranking.removeLast();
                }
            }
        }
        List<PersonalRecord> result = new ArrayList<>();
        for (int i = 0; i < DISTANCES_KM.size(); i++) {
            result.add(new PersonalRecord(DISTANCES_KM.get(i), List.copyOf(rankings.get(i))));
        }
        return List.copyOf(result);
    }
}
