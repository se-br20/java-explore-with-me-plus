package ru.practicum.ewm.stats.aggregator.service;

import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.aggregator.model.EventPair;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimilarityService {

    private final Map<Long, Map<Long, Double>>
            userEventWeights = new HashMap<>();

    private final Map<Long, Double>
            eventWeightSums = new HashMap<>();

    private final Map<EventPair, Double>
            pairMinWeightSums = new HashMap<>();

    public synchronized List<EventSimilarityAvro> update(
            UserActionAvro action
    ) {
        long userId = action.getUserId();
        long updatedEventId = action.getEventId();

        double receivedWeight =
                resolveWeight(action.getActionType());

        Map<Long, Double> currentUserWeights =
                userEventWeights.computeIfAbsent(
                        userId,
                        ignored -> new HashMap<>()
                );

        double oldWeight =
                currentUserWeights.getOrDefault(
                        updatedEventId,
                        0.0
                );

        double newWeight =
                Math.max(oldWeight, receivedWeight);

        if (Double.compare(newWeight, oldWeight) <= 0) {
            return List.of();
        }

        List<Long> knownEventIds =
                new ArrayList<>(
                        eventWeightSums.keySet()
                );

        double eventWeightDelta =
                newWeight - oldWeight;

        eventWeightSums.merge(
                updatedEventId,
                eventWeightDelta,
                Double::sum
        );

        List<EventSimilarityAvro> similarities =
                new ArrayList<>();

        for (Long otherEventId : knownEventIds) {
            if (otherEventId == updatedEventId) {
                continue;
            }

            double otherEventWeight =
                    currentUserWeights.getOrDefault(
                            otherEventId,
                            0.0
                    );

            EventPair pair = EventPair.of(
                    updatedEventId,
                    otherEventId
            );

            double oldMinimum =
                    Math.min(
                            oldWeight,
                            otherEventWeight
                    );

            double newMinimum =
                    Math.min(
                            newWeight,
                            otherEventWeight
                    );

            double minimumDelta =
                    newMinimum - oldMinimum;

            pairMinWeightSums.merge(
                    pair,
                    minimumDelta,
                    Double::sum
            );

            double score =
                    calculateSimilarity(pair);

            similarities.add(
                    EventSimilarityAvro.newBuilder()
                            .setEventA(pair.eventA())
                            .setEventB(pair.eventB())
                            .setScore(score)
                            .setTimestamp(
                                    action.getTimestamp()
                            )
                            .build()
            );
        }

        currentUserWeights.put(
                updatedEventId,
                newWeight
        );

        return similarities;
    }

    private double calculateSimilarity(
            EventPair pair
    ) {
        double minimumSum =
                pairMinWeightSums.getOrDefault(
                        pair,
                        0.0
                );

        double eventASum =
                eventWeightSums.getOrDefault(
                        pair.eventA(),
                        0.0
                );

        double eventBSum =
                eventWeightSums.getOrDefault(
                        pair.eventB(),
                        0.0
                );

        double denominator =
                Math.sqrt(eventASum)
                        * Math.sqrt(eventBSum);

        if (denominator == 0.0) {
            return 0.0;
        }

        return minimumSum / denominator;
    }

    private double resolveWeight(
            ActionTypeAvro actionType
    ) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}