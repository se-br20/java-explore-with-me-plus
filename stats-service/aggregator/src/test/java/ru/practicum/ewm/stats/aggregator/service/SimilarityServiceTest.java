package ru.practicum.ewm.stats.aggregator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimilarityServiceTest {

    private SimilarityService similarityService;

    @BeforeEach
    void setUp() {
        similarityService = new SimilarityService();
    }

    @Test
    void shouldUseMaximumWeightAndOrderEventIds() {

        List<EventSimilarityAvro> firstResult =
                similarityService.update(
                        action(
                                1L,
                                20L,
                                ActionTypeAvro.VIEW
                        )
                );

        assertTrue(firstResult.isEmpty());

        List<EventSimilarityAvro> secondResult =
                similarityService.update(
                        action(
                                1L,
                                10L,
                                ActionTypeAvro.VIEW
                        )
                );

        assertEquals(1, secondResult.size());

        EventSimilarityAvro initialSimilarity =
                secondResult.getFirst();

        assertEquals(
                10L,
                initialSimilarity.getEventA()
        );

        assertEquals(
                20L,
                initialSimilarity.getEventB()
        );

        assertEquals(
                1.0,
                initialSimilarity.getScore(),
                0.000001
        );

        List<EventSimilarityAvro> upgradedResult =
                similarityService.update(
                        action(
                                1L,
                                20L,
                                ActionTypeAvro.REGISTER
                        )
                );

        assertEquals(1, upgradedResult.size());

        assertEquals(
                0.707106,
                upgradedResult.getFirst().getScore(),
                0.000001
        );

        List<EventSimilarityAvro> lowerWeightResult =
                similarityService.update(
                        action(
                                1L,
                                20L,
                                ActionTypeAvro.VIEW
                        )
                );

        assertTrue(lowerWeightResult.isEmpty());
    }

    private UserActionAvro action(
            Long userId,
            Long eventId,
            ActionTypeAvro actionType
    ) {
        return UserActionAvro.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Instant.now())
                .build();
    }
}