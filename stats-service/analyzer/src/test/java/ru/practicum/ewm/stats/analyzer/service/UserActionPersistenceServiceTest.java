package ru.practicum.ewm.stats.analyzer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.practicum.ewm.stats.analyzer.model.UserActionEntity;
import ru.practicum.ewm.stats.analyzer.model.UserActionId;
import ru.practicum.ewm.stats.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserActionPersistenceServiceTest {

    private UserActionRepository repository;
    private UserActionPersistenceService service;

    @BeforeEach
    void setUp() {
        repository = mock(
                UserActionRepository.class
        );

        service = new UserActionPersistenceService(
                repository
        );
    }

    @Test
    void shouldSaveNewInteraction() {
        UserActionId id =
                new UserActionId(1L, 10L);

        when(repository.findById(id))
                .thenReturn(Optional.empty());

        Instant timestamp =
                Instant.parse(
                        "2026-07-20T10:00:00Z"
                );

        service.process(
                action(
                        1L,
                        10L,
                        ActionTypeAvro.REGISTER,
                        timestamp
                )
        );

        ArgumentCaptor<UserActionEntity> captor =
                ArgumentCaptor.forClass(
                        UserActionEntity.class
                );

        verify(repository).save(captor.capture());

        UserActionEntity saved =
                captor.getValue();

        assertEquals(1L, saved.getId().getUserId());
        assertEquals(10L, saved.getId().getEventId());
        assertEquals(0.8, saved.getRating());
        assertEquals(
                timestamp,
                saved.getLastInteractionAt()
        );
    }

    @Test
    void shouldKeepMaximumRatingAndUpdateTime() {
        Instant oldTimestamp =
                Instant.parse(
                        "2026-07-20T10:00:00Z"
                );

        Instant newTimestamp =
                Instant.parse(
                        "2026-07-20T11:00:00Z"
                );

        UserActionId id =
                new UserActionId(1L, 10L);

        UserActionEntity existing =
                UserActionEntity.builder()
                        .id(id)
                        .rating(0.8)
                        .lastInteractionAt(oldTimestamp)
                        .build();

        when(repository.findById(id))
                .thenReturn(Optional.of(existing));

        service.process(
                action(
                        1L,
                        10L,
                        ActionTypeAvro.VIEW,
                        newTimestamp
                )
        );

        assertEquals(0.8, existing.getRating());

        assertEquals(
                newTimestamp,
                existing.getLastInteractionAt()
        );

        verify(repository).save(existing);
    }

    private UserActionAvro action(
            Long userId,
            Long eventId,
            ActionTypeAvro type,
            Instant timestamp
    ) {
        return UserActionAvro.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(type)
                .setTimestamp(timestamp)
                .build();
    }
}