package ru.practicum.ewm.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.categories.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.interaction.CommentCountProvider;
import ru.practicum.ewm.interaction.RequestCountProvider;
import ru.practicum.ewm.interaction.client.UserServiceClient;
import ru.practicum.stat.client.AnalyzerClient;
import ru.practicum.stat.client.CollectorClient;
import ru.practicum.stat.client.RecommendedEvent;
import ru.practicum.stat.client.UserActionType;
import ru.practicum.stat.client.StatsClient;
import ru.practicum.stat.dto.ParamDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventServiceImplViewActionTest {

    private EventRepository eventRepository;
    private CollectorClient collectorClient;
    private AnalyzerClient analyzerClient;
    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {

        StatsClient statsClient =
                mock(StatsClient.class);

        when(
                statsClient.get(
                        any(ParamDto.class)
                )
        ).thenReturn(List.of());

        eventRepository =
                mock(EventRepository.class);

        UserServiceClient userServiceClient =
                mock(UserServiceClient.class);

        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        collectorClient =
                mock(CollectorClient.class);

        analyzerClient =
                mock(AnalyzerClient.class);

        CommentCountProvider commentCountProvider =
                mock(CommentCountProvider.class);

        RequestCountProvider requestCountProvider =
                mock(RequestCountProvider.class);

        when(
                analyzerClient.getInteractionsCount(
                        anyCollection()
                )
        ).thenReturn(
                List.of(
                        new RecommendedEvent(
                                10L,
                                0.4
                        )
                )
        );

        eventService = new EventServiceImpl(
                eventRepository,
                userServiceClient,
                categoryRepository,
                collectorClient,
                analyzerClient,
                commentCountProvider,
                requestCountProvider,
                mock(StatsClient.class)
        );
    }

    @Test
    void shouldSendViewWhenUserIdProvided() {
        EventFullDto event =
                publishedEvent(10L);

        when(
                eventRepository.findEventByIdFullDto(
                        10L
                )
        ).thenReturn(
                Optional.of(event)
        );

        EventFullDto result =
                eventService.findEventById(
                        "/events/10",
                        "127.0.0.1",
                        10L,
                        5L
                );

        assertSame(event, result);

        assertEquals(
                0.4,
                result.getRating(),
                0.000001
        );

        verify(analyzerClient)
                .getInteractionsCount(
                        List.of(10L)
                );

        verify(collectorClient)
                .sendAction(
                        5L,
                        10L,
                        UserActionType.VIEW
                );
    }

    @Test
    void shouldReturnEmptyRecommendationsWhenAnalyzerHasNoData() {
        when(
                analyzerClient.getRecommendationsForUser(
                        5L,
                        10
                )
        ).thenReturn(List.of());

        List<EventShortDto> result =
                eventService.getRecommendations(
                        5L
                );

        assertTrue(result.isEmpty());

        verify(analyzerClient)
                .getRecommendationsForUser(
                        5L,
                        10
                );

        verify(eventRepository, never())
                .findAllByIdIn(
                        anyCollection()
                );
    }

    private EventFullDto publishedEvent(
            Long eventId
    ) {
        return EventFullDto.builder()
                .id(eventId)
                .state(EventState.PUBLISHED)
                .publishedOn(
                        LocalDateTime.of(
                                2026,
                                7,
                                20,
                                12,
                                0
                        )
                )
                .build();
    }
}