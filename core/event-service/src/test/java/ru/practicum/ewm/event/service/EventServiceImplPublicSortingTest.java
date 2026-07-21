package ru.practicum.ewm.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.categories.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.paramDto.EventRepositoryParam;
import ru.practicum.ewm.event.dto.paramDto.PublicUserEventParam;
import ru.practicum.ewm.event.model.EventSort;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.interaction.CommentCountProvider;
import ru.practicum.ewm.interaction.RequestCountProvider;
import ru.practicum.ewm.interaction.client.UserServiceClient;
import ru.practicum.stat.client.AnalyzerClient;
import ru.practicum.stat.client.CollectorClient;
import ru.practicum.stat.client.RecommendedEvent;
import ru.practicum.stat.client.StatsClient;
import ru.practicum.stat.dto.ParamDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventServiceImplPublicSortingTest {

    private EventRepository eventRepository;
    private AnalyzerClient analyzerClient;
    private StatsClient statsClient;
    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        eventRepository =
                mock(EventRepository.class);

        analyzerClient =
                mock(AnalyzerClient.class);

        statsClient =
                mock(StatsClient.class);

        /*
         * В этом тесте просмотры не проверяются.
         * Возвращаем пустую статистику, чтобы views стало 0.
         */
        when(
                statsClient.get(
                        any(ParamDto.class)
                )
        ).thenReturn(List.of());

        eventService =
                new EventServiceImpl(
                        eventRepository,
                        mock(UserServiceClient.class),
                        mock(CategoryRepository.class),
                        mock(CollectorClient.class),
                        analyzerClient,
                        mock(CommentCountProvider.class),
                        mock(RequestCountProvider.class),
                        mock(StatsClient.class)
                );
    }

    @Test
    void shouldSortAllEventsBeforeApplyingPagination() {
        List<EventShortDto> events =
                List.of(
                        event(1L, 10),
                        event(2L, 20),
                        event(3L, 30),
                        event(4L, 40)
                );

        when(
                eventRepository
                        .findEventsShortDtoWithoutPagination(
                                any(EventRepositoryParam.class)
                        )
        ).thenReturn(events);

        when(
                analyzerClient
                        .getInteractionsCount(
                                anyCollection()
                        )
        ).thenReturn(
                List.of(
                        new RecommendedEvent(
                                1L,
                                0.1
                        ),
                        new RecommendedEvent(
                                2L,
                                0.9
                        ),
                        new RecommendedEvent(
                                3L,
                                0.4
                        ),
                        new RecommendedEvent(
                                4L,
                                0.8
                        )
                )
        );

        PublicUserEventParam param =
                PublicUserEventParam.builder()
                        .sort(EventSort.RATING)
                        .onlyAvailable(false)
                        .from(1)
                        .size(2)
                        .build();

        List<EventShortDto> result =
                eventService
                        .getEventsForPublicRequests(
                                param
                        );

        assertEquals(
                List.of(4L, 3L),
                result.stream()
                        .map(EventShortDto::getId)
                        .toList()
        );

        verify(eventRepository)
                .findEventsShortDtoWithoutPagination(
                        any(EventRepositoryParam.class)
                );

        verify(eventRepository, never())
                .findEventsShortDto(
                        any(EventRepositoryParam.class)
                );
    }

    private EventShortDto event(
            Long id,
            long dayOffset
    ) {
        LocalDateTime eventDate =
                LocalDateTime.of(
                                2027,
                                1,
                                1,
                                12,
                                0
                        )
                        .plusDays(dayOffset);

        return EventShortDto.builder()
                .id(id)
                .eventDate(eventDate)
                .publishedOn(
                        LocalDateTime.of(
                                2026,
                                1,
                                1,
                                12,
                                0
                        )
                )
                .rating(0.0)
                .views(0L)
                .confirmedRequests(0L)
                .commentsCount(0L)
                .build();
    }
}