package ru.practicum.ewm.event.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.categories.repository.CategoryRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.exceptions.ConditionsNotMetException;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.ewm.interaction.CommentCountProvider;
import ru.practicum.ewm.interaction.RequestCountProvider;
import ru.practicum.ewm.interaction.client.UserServiceClient;
import ru.practicum.stat.client.AnalyzerClient;
import ru.practicum.stat.client.CollectorClient;
import ru.practicum.stat.client.UserActionType;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplLikeActionTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CollectorClient collectorClient;

    @Mock
    private AnalyzerClient analyzerClient;

    @Mock
    private CommentCountProvider commentCountProvider;

    @Mock
    private RequestCountProvider requestCountProvider;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void shouldSendLikeWhenUserHasConfirmedRequest() {
        long userId = 1L;
        long eventId = 2L;

        Event event =
                Event.builder()
                        .id(eventId)
                        .state(
                                EventState.PUBLISHED
                        )
                        .build();

        when(eventRepository.findById(eventId))
                .thenReturn(
                        Optional.of(event)
                );

        when(
                requestCountProvider
                        .hasConfirmedRequest(
                                userId,
                                eventId
                        )
        ).thenReturn(true);

        eventService.likeEvent(
                userId,
                eventId
        );

        verify(requestCountProvider)
                .hasConfirmedRequest(
                        userId,
                        eventId
                );

        verify(collectorClient)
                .sendAction(
                        userId,
                        eventId,
                        UserActionType.LIKE
                );
    }

    @Test
    void shouldRejectLikeWithoutConfirmedRequest() {
        long userId = 3L;
        long eventId = 2L;

        Event event =
                Event.builder()
                        .id(eventId)
                        .state(
                                EventState.PUBLISHED
                        )
                        .build();

        when(eventRepository.findById(eventId))
                .thenReturn(
                        Optional.of(event)
                );

        when(
                requestCountProvider
                        .hasConfirmedRequest(
                                userId,
                                eventId
                        )
        ).thenReturn(false);

        assertThrows(
                ConditionsNotMetException.class,
                () -> eventService.likeEvent(
                        userId,
                        eventId
                )
        );

        verify(collectorClient, never())
                .sendAction(
                        userId,
                        eventId,
                        UserActionType.LIKE
                );
    }

    @Test
    void shouldRejectLikeForUnpublishedEvent() {
        long userId = 1L;
        long eventId = 2L;

        Event event =
                Event.builder()
                        .id(eventId)
                        .state(
                                EventState.PENDING
                        )
                        .build();

        when(eventRepository.findById(eventId))
                .thenReturn(
                        Optional.of(event)
                );

        assertThrows(
                NotFoundException.class,
                () -> eventService.likeEvent(
                        userId,
                        eventId
                )
        );

        verify(requestCountProvider, never())
                .hasConfirmedRequest(
                        userId,
                        eventId
                );

        verify(collectorClient, never())
                .sendAction(
                        userId,
                        eventId,
                        UserActionType.LIKE
                );
    }
}