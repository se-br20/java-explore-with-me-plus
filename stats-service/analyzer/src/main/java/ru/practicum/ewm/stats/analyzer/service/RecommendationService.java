package ru.practicum.ewm.stats.analyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.dto.Recommendation;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stats.analyzer.model.UserActionEntity;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserActionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private static final Comparator<Recommendation>
            RECOMMENDATION_ORDER =
            Comparator.comparingDouble(
                            Recommendation::score
                    )
                    .reversed()
                    .thenComparingLong(
                            Recommendation::eventId
                    );

    private final UserActionRepository
            userActionRepository;

    private final EventSimilarityRepository
            similarityRepository;

    private final int recentActionsLimit;

    public RecommendationService(
            UserActionRepository userActionRepository,
            EventSimilarityRepository
                    similarityRepository,

            @Value(
                    "${recommendations.recent-actions-limit:5}"
            )
            int recentActionsLimit
    ) {
        if (recentActionsLimit <= 0) {
            throw new IllegalArgumentException(
                    "recentActionsLimit must be positive"
            );
        }

        this.userActionRepository =
                userActionRepository;

        this.similarityRepository =
                similarityRepository;

        this.recentActionsLimit =
                recentActionsLimit;
    }

    public List<Recommendation>
    getRecommendationsForUser(
            long userId,
            int maxResults
    ) {
        validatePositiveId(userId, "userId");
        validateMaxResults(maxResults);

        List<UserActionEntity> allUserActions =
                userActionRepository
                        .findAllByUserIdOrderByInteractionTimeDesc(
                                userId
                        );

        if (allUserActions.isEmpty()) {
            return List.of();
        }

        Set<Long> interactedEventIds =
                allUserActions.stream()
                        .map(action ->
                                action.getId()
                                        .getEventId()
                        )
                        .collect(
                                Collectors.toCollection(
                                        HashSet::new
                                )
                        );

        Map<Long, Double> recentRatings =
                extractRecentRatings(
                        allUserActions
                );

        if (recentRatings.isEmpty()) {
            return List.of();
        }

        List<EventSimilarityEntity> similarities =
                similarityRepository
                        .findAllConnectedToAnyEvent(
                                recentRatings.keySet()
                        );

        Map<Long, PredictionAccumulator>
                predictions =
                new HashMap<>();

        for (EventSimilarityEntity similarity
                : similarities) {

            long eventA =
                    similarity.getId().getEventA();

            long eventB =
                    similarity.getId().getEventB();

            double similarityScore =
                    similarity.getScore();

            addPredictionContribution(
                    eventA,
                    eventB,
                    similarityScore,
                    recentRatings,
                    interactedEventIds,
                    predictions
            );

            addPredictionContribution(
                    eventB,
                    eventA,
                    similarityScore,
                    recentRatings,
                    interactedEventIds,
                    predictions
            );
        }

        return predictions.entrySet()
                .stream()
                .map(entry ->
                        new Recommendation(
                                entry.getKey(),
                                entry.getValue()
                                        .calculateScore()
                        )
                )
                .filter(recommendation ->
                        Double.isFinite(
                                recommendation.score()
                        )
                )
                .sorted(RECOMMENDATION_ORDER)
                .limit(maxResults)
                .toList();
    }

    public List<Recommendation> getSimilarEvents(
            long eventId,
            long userId,
            int maxResults
    ) {
        validatePositiveId(eventId, "eventId");
        validatePositiveId(userId, "userId");
        validateMaxResults(maxResults);

        Set<Long> interactedEventIds =
                new HashSet<>(
                        userActionRepository
                                .findEventIdsByUserId(
                                        userId
                                )
                );

        return similarityRepository
                .findAllConnectedToEvent(eventId)
                .stream()
                .map(similarity ->
                        toSimilarEventRecommendation(
                                eventId,
                                similarity
                        )
                )
                .filter(recommendation ->
                        !interactedEventIds.contains(
                                recommendation.eventId()
                        )
                )
                .sorted(RECOMMENDATION_ORDER)
                .limit(maxResults)
                .toList();
    }

    public List<Recommendation>
    getInteractionsCount(
            Collection<Long> eventIds
    ) {
        if (eventIds == null
                || eventIds.isEmpty()) {

            return List.of();
        }

        LinkedHashSet<Long> uniqueEventIds =
                new LinkedHashSet<>();

        for (Long eventId : eventIds) {
            if (eventId == null) {
                throw new IllegalArgumentException(
                        "eventId must not be null"
                );
            }

            validatePositiveId(
                    eventId,
                    "eventId"
            );

            uniqueEventIds.add(eventId);
        }

        Map<Long, Double> scoresByEventId =
                userActionRepository
                        .sumRatingsByEventIds(
                                uniqueEventIds
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        UserActionRepository
                                                .EventInteractionScoreProjection
                                                ::getEventId,

                                        projection ->
                                                projection.getScore()
                                                        == null
                                                        ? 0.0
                                                        : projection
                                                        .getScore()
                                )
                        );

        List<Recommendation> result =
                new ArrayList<>(
                        uniqueEventIds.size()
                );

        for (Long eventId : uniqueEventIds) {
            result.add(
                    new Recommendation(
                            eventId,
                            scoresByEventId
                                    .getOrDefault(
                                            eventId,
                                            0.0
                                    )
                    )
            );
        }

        return result;
    }

    private Map<Long, Double> extractRecentRatings(
            List<UserActionEntity> allUserActions
    ) {
        int resultSize =
                Math.min(
                        recentActionsLimit,
                        allUserActions.size()
                );

        Map<Long, Double> recentRatings =
                new LinkedHashMap<>();

        for (int index = 0;
             index < resultSize;
             index++) {

            UserActionEntity action =
                    allUserActions.get(index);

            recentRatings.put(
                    action.getId().getEventId(),
                    action.getRating()
            );
        }

        return recentRatings;
    }

    private void addPredictionContribution(
            long knownEventId,
            long candidateEventId,
            double similarityScore,
            Map<Long, Double> recentRatings,
            Set<Long> interactedEventIds,
            Map<Long, PredictionAccumulator>
                    predictions
    ) {
        Double knownEventRating =
                recentRatings.get(
                        knownEventId
                );

        if (knownEventRating == null) {
            return;
        }

        if (interactedEventIds.contains(
                candidateEventId
        )) {
            return;
        }

        if (!Double.isFinite(similarityScore)
                || similarityScore <= 0.0) {

            return;
        }

        predictions.computeIfAbsent(
                        candidateEventId,
                        ignored ->
                                new PredictionAccumulator()
                )
                .add(
                        knownEventRating,
                        similarityScore
                );
    }

    private Recommendation
    toSimilarEventRecommendation(
            long sourceEventId,
            EventSimilarityEntity similarity
    ) {
        long eventA =
                similarity.getId().getEventA();

        long eventB =
                similarity.getId().getEventB();

        long similarEventId =
                eventA == sourceEventId
                        ? eventB
                        : eventA;

        return new Recommendation(
                similarEventId,
                similarity.getScore()
        );
    }

    private void validatePositiveId(
            long id,
            String fieldName
    ) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be positive"
            );
        }
    }

    private void validateMaxResults(
            int maxResults
    ) {
        if (maxResults <= 0) {
            throw new IllegalArgumentException(
                    "maxResults must be positive"
            );
        }
    }

    private static final class
    PredictionAccumulator {

        private double weightedRatingSum;
        private double similaritySum;

        private void add(
                double userRating,
                double similarity
        ) {
            weightedRatingSum +=
                    userRating * similarity;

            similaritySum += similarity;
        }

        private double calculateScore() {
            if (similaritySum == 0.0) {
                return 0.0;
            }

            return weightedRatingSum
                    / similaritySum;
        }
    }
}