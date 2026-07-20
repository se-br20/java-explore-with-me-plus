package ru.practicum.ewm.stats.analyzer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.analyzer.dto.Recommendation;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityId;
import ru.practicum.ewm.stats.analyzer.model.UserActionEntity;
import ru.practicum.ewm.stats.analyzer.model.UserActionId;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserActionRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    private UserActionRepository userActionRepository;

    private EventSimilarityRepository
            similarityRepository;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        userActionRepository =
                mock(UserActionRepository.class);

        similarityRepository =
                mock(EventSimilarityRepository.class);

        service = new RecommendationService(
                userActionRepository,
                similarityRepository,
                2
        );
    }

    @Test
    void shouldSelectCandidatesBeforePredictionAndUseOnlyNearestNeighbors() {
        List<UserActionEntity> actions =
                List.of(
                        action(
                                1L,
                                1L,
                                1.0,
                                "2026-07-20T12:00:00Z"
                        ),
                        action(
                                1L,
                                2L,
                                0.4,
                                "2026-07-20T11:00:00Z"
                        ),
                        action(
                                1L,
                                3L,
                                0.8,
                                "2026-07-20T10:00:00Z"
                        )
                );

        when(
                userActionRepository
                        .findAllByUserIdOrderByInteractionTimeDesc(
                                1L
                        )
        ).thenReturn(actions);

        when(
                similarityRepository
                        .findAllConnectedToAnyEvent(
                                argThat(eventIds ->
                                        containsExactly(
                                                eventIds,
                                                1L,
                                                2L
                                        )
                                )
                        )
        ).thenReturn(
                List.of(
                        similarity(1L, 10L, 0.90),
                        similarity(2L, 10L, 0.80),

                        similarity(1L, 11L, 0.85),
                        similarity(2L, 11L, 0.84),

                        similarity(1L, 12L, 0.20)
                )
        );

        when(
                similarityRepository
                        .findAllConnectedToAnyEvent(
                                argThat(eventIds ->
                                        containsExactly(
                                                eventIds,
                                                10L,
                                                11L
                                        )
                                )
                        )
        ).thenReturn(
                List.of(
                        similarity(1L, 10L, 0.90),
                        similarity(2L, 10L, 0.80),
                        similarity(3L, 10L, 0.10),

                        similarity(1L, 11L, 0.85),
                        similarity(2L, 11L, 0.84),
                        similarity(3L, 11L, 0.83)
                )
        );

        List<Recommendation> result =
                service.getRecommendationsForUser(
                        1L,
                        2
                );

        assertEquals(2, result.size());

        assertEquals(
                10L,
                result.get(0).eventId()
        );

        assertEquals(
                0.7176470588,
                result.get(0).score(),
                0.000001
        );

        assertEquals(
                11L,
                result.get(1).eventId()
        );

        assertEquals(
                0.7017751479,
                result.get(1).score(),
                0.000001
        );
    }

    @Test
    void shouldReturnEmptyRecommendationsWithoutUserActions() {
        when(
                userActionRepository
                        .findAllByUserIdOrderByInteractionTimeDesc(
                                1L
                        )
        ).thenReturn(List.of());

        List<Recommendation> result =
                service.getRecommendationsForUser(
                        1L,
                        10
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldExcludeAlreadyInteractedSimilarEvents() {
        when(
                userActionRepository
                        .findEventIdsByUserId(1L)
        ).thenReturn(
                List.of(2L)
        );

        when(
                similarityRepository
                        .findAllConnectedToEvent(1L)
        ).thenReturn(
                List.of(
                        similarity(
                                1L,
                                2L,
                                0.9
                        ),
                        similarity(
                                1L,
                                3L,
                                0.8
                        ),
                        similarity(
                                1L,
                                4L,
                                0.7
                        )
                )
        );

        List<Recommendation> result =
                service.getSimilarEvents(
                        1L,
                        1L,
                        2
                );

        assertEquals(2, result.size());

        assertEquals(
                3L,
                result.get(0).eventId()
        );

        assertEquals(
                0.8,
                result.get(0).score(),
                0.000001
        );

        assertEquals(
                4L,
                result.get(1).eventId()
        );

        assertEquals(
                0.7,
                result.get(1).score(),
                0.000001
        );
    }

    @Test
    void shouldReturnInteractionScoresForAllRequestedEvents() {
        UserActionRepository
                .EventInteractionScoreProjection
                eventOneProjection =
                projection(
                        1L,
                        1.8
                );

        UserActionRepository
                .EventInteractionScoreProjection
                eventTwoProjection =
                projection(
                        2L,
                        0.4
                );

        when(
                userActionRepository
                        .sumRatingsByEventIds(
                                argThat(eventIds ->
                                        containsExactly(
                                                eventIds,
                                                1L,
                                                2L,
                                                3L
                                        )
                                )
                        )
        ).thenReturn(
                List.of(
                        eventOneProjection,
                        eventTwoProjection
                )
        );

        List<Recommendation> result =
                service.getInteractionsCount(
                        List.of(
                                2L,
                                1L,
                                2L,
                                3L
                        )
                );

        assertEquals(3, result.size());

        assertEquals(
                2L,
                result.get(0).eventId()
        );

        assertEquals(
                0.4,
                result.get(0).score(),
                0.000001
        );

        assertEquals(
                1L,
                result.get(1).eventId()
        );

        assertEquals(
                1.8,
                result.get(1).score(),
                0.000001
        );

        assertEquals(
                3L,
                result.get(2).eventId()
        );

        assertEquals(
                0.0,
                result.get(2).score(),
                0.000001
        );
    }

    private boolean containsExactly(
            Collection<Long> actual,
            Long... expected
    ) {
        return actual != null
                && actual.size() == expected.length
                && actual.containsAll(
                List.of(expected)
        );
    }

    private UserActionEntity action(
            Long userId,
            Long eventId,
            Double rating,
            String timestamp
    ) {
        return UserActionEntity.builder()
                .id(
                        new UserActionId(
                                userId,
                                eventId
                        )
                )
                .rating(rating)
                .lastInteractionAt(
                        Instant.parse(timestamp)
                )
                .build();
    }

    private EventSimilarityEntity similarity(
            Long eventA,
            Long eventB,
            Double score
    ) {
        return EventSimilarityEntity.builder()
                .id(
                        new EventSimilarityId(
                                eventA,
                                eventB
                        )
                )
                .score(score)
                .updatedAt(
                        Instant.parse(
                                "2026-07-20T12:00:00Z"
                        )
                )
                .build();
    }

    private UserActionRepository
            .EventInteractionScoreProjection
    projection(
            Long eventId,
            Double score
    ) {
        UserActionRepository
                .EventInteractionScoreProjection
                projection =
                mock(
                        UserActionRepository
                                .EventInteractionScoreProjection
                                .class
                );

        when(projection.getEventId())
                .thenReturn(eventId);

        when(projection.getScore())
                .thenReturn(score);

        return projection;
    }
}