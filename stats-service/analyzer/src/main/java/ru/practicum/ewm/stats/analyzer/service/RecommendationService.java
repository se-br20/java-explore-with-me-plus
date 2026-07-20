package ru.practicum.ewm.stats.analyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.dto.Recommendation;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stats.analyzer.model.UserActionEntity;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserActionRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    private static final Comparator<Candidate>
            CANDIDATE_ORDER =
            Comparator.comparingDouble(
                            Candidate::similarity
                    )
                    .reversed()
                    .thenComparingLong(
                            Candidate::eventId
                    );

    private static final Comparator<Neighbor>
            NEIGHBOR_ORDER =
            Comparator.comparingDouble(
                            Neighbor::similarity
                    )
                    .reversed()
                    .thenComparingLong(
                            Neighbor::eventId
                    );

    private final UserActionRepository userActionRepository;

    private final EventSimilarityRepository similarityRepository;

    private final int nearestNeighborsLimit;

    public RecommendationService(
            UserActionRepository userActionRepository,
            EventSimilarityRepository similarityRepository,

            @Value(
                    "${recommendations.nearest-neighbors-limit:5}"
            )
            int nearestNeighborsLimit
    ) {
        if (nearestNeighborsLimit <= 0) {
            throw new IllegalArgumentException(
                    "nearestNeighborsLimit must be positive"
            );
        }

        this.userActionRepository =
                userActionRepository;

        this.similarityRepository =
                similarityRepository;

        this.nearestNeighborsLimit =
                nearestNeighborsLimit;
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
                                action.getId().getEventId()
                        )
                        .collect(
                                Collectors.toCollection(
                                        HashSet::new
                                )
                        );

        Map<Long, Double> ratingsByEventId =
                extractRatings(allUserActions);

        Set<Long> recentEventIds =
                extractRecentEventIds(
                        allUserActions,
                        maxResults
                );

        if (recentEventIds.isEmpty()) {
            return List.of();
        }

        List<EventSimilarityEntity>
                candidateSimilarities =
                similarityRepository
                        .findAllConnectedToAnyEvent(
                                recentEventIds
                        );

        List<Long> candidateEventIds =
                selectCandidateEventIds(
                        candidateSimilarities,
                        recentEventIds,
                        interactedEventIds,
                        maxResults
                );

        if (candidateEventIds.isEmpty()) {
            return List.of();
        }

        List<EventSimilarityEntity>
                neighborSimilarities =
                similarityRepository
                        .findAllConnectedToAnyEvent(
                                candidateEventIds
                        );

        Map<Long, List<Neighbor>>
                neighborsByCandidate =
                buildNeighborsByCandidate(
                        candidateEventIds,
                        ratingsByEventId,
                        neighborSimilarities
                );

        List<Recommendation> recommendations =
                new ArrayList<>();

        for (Long candidateEventId
                : candidateEventIds) {

            calculatePrediction(
                    candidateEventId,
                    neighborsByCandidate
                            .getOrDefault(
                                    candidateEventId,
                                    List.of()
                            )
            ).ifPresent(recommendations::add);
        }

        return recommendations.stream()
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
                .filter(recommendation ->
                        isPositiveFinite(
                                recommendation.score()
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

    private Map<Long, Double> extractRatings(
            List<UserActionEntity> actions
    ) {
        Map<Long, Double> ratings =
                new LinkedHashMap<>();

        for (UserActionEntity action : actions) {
            Double rating = action.getRating();

            if (rating == null
                    || !Double.isFinite(rating)
                    || rating <= 0.0) {

                continue;
            }

            ratings.put(
                    action.getId().getEventId(),
                    rating
            );
        }

        return ratings;
    }

    private Set<Long> extractRecentEventIds(
            List<UserActionEntity> allUserActions,
            int maxResults
    ) {
        int resultSize =
                Math.min(
                        maxResults,
                        allUserActions.size()
                );

        Set<Long> recentEventIds =
                new LinkedHashSet<>();

        for (int index = 0;
             index < resultSize;
             index++) {

            recentEventIds.add(
                    allUserActions
                            .get(index)
                            .getId()
                            .getEventId()
            );
        }

        return recentEventIds;
    }

    private List<Long> selectCandidateEventIds(
            List<EventSimilarityEntity> similarities,
            Set<Long> recentEventIds,
            Set<Long> interactedEventIds,
            int maxResults
    ) {
        Map<Long, Double>
                maxSimilarityByCandidate =
                new HashMap<>();

        for (EventSimilarityEntity similarity
                : similarities) {

            long eventA =
                    similarity.getId().getEventA();

            long eventB =
                    similarity.getId().getEventB();

            double score =
                    similarity.getScore();

            registerCandidate(
                    eventA,
                    eventB,
                    score,
                    recentEventIds,
                    interactedEventIds,
                    maxSimilarityByCandidate
            );

            registerCandidate(
                    eventB,
                    eventA,
                    score,
                    recentEventIds,
                    interactedEventIds,
                    maxSimilarityByCandidate
            );
        }

        return maxSimilarityByCandidate
                .entrySet()
                .stream()
                .map(entry ->
                        new Candidate(
                                entry.getKey(),
                                entry.getValue()
                        )
                )
                .sorted(CANDIDATE_ORDER)
                .limit(maxResults)
                .map(Candidate::eventId)
                .toList();
    }

    private void registerCandidate(
            long knownEventId,
            long candidateEventId,
            double similarity,
            Set<Long> recentEventIds,
            Set<Long> interactedEventIds,
            Map<Long, Double>
                    maxSimilarityByCandidate
    ) {
        if (!recentEventIds.contains(
                knownEventId
        )) {
            return;
        }

        if (interactedEventIds.contains(
                candidateEventId
        )) {
            return;
        }

        if (!isPositiveFinite(similarity)) {
            return;
        }

        maxSimilarityByCandidate.merge(
                candidateEventId,
                similarity,
                Math::max
        );
    }

    private Map<Long, List<Neighbor>>
    buildNeighborsByCandidate(
            Collection<Long> candidateEventIds,
            Map<Long, Double> ratingsByEventId,
            List<EventSimilarityEntity> similarities
    ) {
        Set<Long> candidateIdSet =
                new HashSet<>(candidateEventIds);

        Map<Long, List<Neighbor>>
                neighborsByCandidate =
                new HashMap<>();

        for (EventSimilarityEntity similarity
                : similarities) {

            long eventA =
                    similarity.getId().getEventA();

            long eventB =
                    similarity.getId().getEventB();

            double score =
                    similarity.getScore();

            registerNeighbor(
                    eventA,
                    eventB,
                    score,
                    candidateIdSet,
                    ratingsByEventId,
                    neighborsByCandidate
            );

            registerNeighbor(
                    eventB,
                    eventA,
                    score,
                    candidateIdSet,
                    ratingsByEventId,
                    neighborsByCandidate
            );
        }

        return neighborsByCandidate;
    }

    private void registerNeighbor(
            long candidateEventId,
            long knownEventId,
            double similarity,
            Set<Long> candidateEventIds,
            Map<Long, Double> ratingsByEventId,
            Map<Long, List<Neighbor>>
                    neighborsByCandidate
    ) {
        if (!candidateEventIds.contains(
                candidateEventId
        )) {
            return;
        }

        Double userRating =
                ratingsByEventId.get(
                        knownEventId
                );

        if (userRating == null) {
            return;
        }

        if (!isPositiveFinite(similarity)) {
            return;
        }

        neighborsByCandidate
                .computeIfAbsent(
                        candidateEventId,
                        ignored ->
                                new ArrayList<>()
                )
                .add(
                        new Neighbor(
                                knownEventId,
                                userRating,
                                similarity
                        )
                );
    }

    private Optional<Recommendation>
    calculatePrediction(
            long candidateEventId,
            List<Neighbor> availableNeighbors
    ) {
        List<Neighbor> nearestNeighbors =
                availableNeighbors.stream()
                        .sorted(NEIGHBOR_ORDER)
                        .limit(nearestNeighborsLimit)
                        .toList();

        PredictionAccumulator accumulator =
                new PredictionAccumulator();

        for (Neighbor neighbor
                : nearestNeighbors) {

            accumulator.add(
                    neighbor.rating(),
                    neighbor.similarity()
            );
        }

        if (!accumulator.hasData()) {
            return Optional.empty();
        }

        double predictedScore =
                accumulator.calculateScore();

        if (!Double.isFinite(predictedScore)) {
            return Optional.empty();
        }

        return Optional.of(
                new Recommendation(
                        candidateEventId,
                        predictedScore
                )
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

    private boolean isPositiveFinite(
            double value
    ) {
        return Double.isFinite(value)
                && value > 0.0;
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

    private record Candidate(
            long eventId,
            double similarity
    ) {
    }

    private record Neighbor(
            long eventId,
            double rating,
            double similarity
    ) {
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

        private boolean hasData() {
            return similaritySum > 0.0;
        }

        private double calculateScore() {
            return weightedRatingSum
                    / similaritySum;
        }
    }
}