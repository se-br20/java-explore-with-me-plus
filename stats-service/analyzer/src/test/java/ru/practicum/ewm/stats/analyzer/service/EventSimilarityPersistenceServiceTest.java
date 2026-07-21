package ru.practicum.ewm.stats.analyzer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityId;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventSimilarityPersistenceServiceTest {

    private EventSimilarityRepository repository;

    private EventSimilarityPersistenceService service;

    @BeforeEach
    void setUp() {
        repository = mock(
                EventSimilarityRepository.class
        );

        service =
                new EventSimilarityPersistenceService(
                        repository
                );
    }

    @Test
    void shouldOrderIdsAndSaveNewSimilarity() {
        EventSimilarityId expectedId =
                new EventSimilarityId(10L, 20L);

        when(repository.findById(expectedId))
                .thenReturn(Optional.empty());

        Instant timestamp =
                Instant.parse(
                        "2026-07-20T10:00:00Z"
                );

        service.process(
                similarity(
                        20L,
                        10L,
                        0.75,
                        timestamp
                )
        );

        ArgumentCaptor<EventSimilarityEntity>
                captor =
                ArgumentCaptor.forClass(
                        EventSimilarityEntity.class
                );

        verify(repository).save(captor.capture());

        EventSimilarityEntity saved =
                captor.getValue();

        assertEquals(
                10L,
                saved.getId().getEventA()
        );

        assertEquals(
                20L,
                saved.getId().getEventB()
        );

        assertEquals(0.75, saved.getScore());

        assertEquals(
                timestamp,
                saved.getUpdatedAt()
        );
    }

    @Test
    void shouldIgnoreOlderSimilarityMessage() {
        EventSimilarityId id =
                new EventSimilarityId(10L, 20L);

        Instant currentTimestamp =
                Instant.parse(
                        "2026-07-20T11:00:00Z"
                );

        EventSimilarityEntity existing =
                EventSimilarityEntity.builder()
                        .id(id)
                        .score(0.8)
                        .updatedAt(currentTimestamp)
                        .build();

        when(repository.findById(id))
                .thenReturn(Optional.of(existing));

        service.process(
                similarity(
                        10L,
                        20L,
                        0.4,
                        Instant.parse(
                                "2026-07-20T10:00:00Z"
                        )
                )
        );

        assertEquals(0.8, existing.getScore());

        verify(repository, never())
                .save(existing);
    }

    private EventSimilarityAvro similarity(
            Long eventA,
            Long eventB,
            Double score,
            Instant timestamp
    ) {
        return EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore(score)
                .setTimestamp(timestamp)
                .build();
    }
}