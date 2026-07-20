package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.categories.model.Category;
import ru.practicum.ewm.categories.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.dto.paramDto.AdminUserEventParam;
import ru.practicum.ewm.event.dto.paramDto.EventRepositoryParam;
import ru.practicum.ewm.event.dto.paramDto.PublicUserEventParam;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventSort;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.exceptions.BadRequestException;
import ru.practicum.ewm.exceptions.exceptions.ConditionsNotMetException;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.ewm.interaction.CommentCountProvider;
import ru.practicum.ewm.interaction.RequestCountProvider;
import ru.practicum.ewm.interaction.client.UserServiceClient;
import ru.practicum.interaction.user.UserDetailsDto;
import ru.practicum.stat.client.AnalyzerClient;
import ru.practicum.stat.client.CollectorClient;
import ru.practicum.stat.client.RecommendedEvent;
import ru.practicum.stat.client.UserActionType;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 10;

    private static final Comparator<EventShortDto>
            RATING_ORDER =
            Comparator.comparing(
                            EventShortDto::getRating,
                            Comparator.nullsLast(
                                    Comparator.reverseOrder()
                            )
                    )
                    .thenComparing(
                            EventShortDto::getEventDate,
                            Comparator.nullsLast(
                                    Comparator.naturalOrder()
                            )
                    )
                    .thenComparing(
                            EventShortDto::getId,
                            Comparator.nullsLast(
                                    Comparator.naturalOrder()
                            )
                    );

    private final EventRepository eventRepository;
    private final UserServiceClient userServiceClient;
    private final CategoryRepository categoryRepository;
    private final CollectorClient collectorClient;
    private final AnalyzerClient analyzerClient;
    private final CommentCountProvider commentCountProvider;
    private final RequestCountProvider requestCountProvider;

    @Override
    @Transactional
    public EventFullDto createEvent(
            Long userId,
            NewEventDto newEventDto
    ) {
        LocalDateTime minEventDate =
                LocalDateTime.now().plusHours(2);

        if (newEventDto.getEventDate()
                .isBefore(minEventDate)) {

            throw new ConditionsNotMetException(
                    "Unable to create event less than "
                            + "2 hours before event date"
            );
        }

        UserDetailsDto user =
                userServiceClient.getUser(userId);

        Category category =
                categoryRepository.getCategory(
                        newEventDto.getCategory()
                );

        Event event =
                EventMapper.toEvent(
                        newEventDto,
                        category,
                        user
                );

        event = eventRepository.save(event);

        return EventMapper.toEventFullDto(
                event,
                0L,
                0.0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(
            Long userId,
            int from,
            int size
    ) {
        EventRepositoryParam param =
                EventRepositoryParam.builder()
                        .users(List.of(userId))
                        .from(from)
                        .size(size)
                        .build();

        List<EventShortDto> events =
                eventRepository.findEventsShortDto(param);

        if (events.isEmpty()) {
            return events;
        }

        enrichEventsWithRatings(events);
        enrichEventsListWithCounts(events);

        return events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsForPublicRequests(
            PublicUserEventParam userEventParam
    ) {
        EventRepositoryParam param =
                EventRepositoryParam.fromUserEventParam(
                        userEventParam
                );

        boolean onlyAvailable =
                Boolean.TRUE.equals(
                        userEventParam.getOnlyAvailable()
                );

        boolean sortByRating =
                param.getSortOrDefault()
                        == EventSort.RATING;

        boolean requiresInMemoryPagination =
                sortByRating || onlyAvailable;

        List<EventShortDto> repositoryResult =
                requiresInMemoryPagination
                        ? eventRepository
                        .findEventsShortDtoWithoutPagination(
                                param
                        )
                        : eventRepository
                        .findEventsShortDto(param);

        if (repositoryResult.isEmpty()) {
            return repositoryResult;
        }

        List<EventShortDto> events =
                new ArrayList<>(repositoryResult);

        enrichEventsWithRatings(events);
        enrichEventsListWithCounts(events);

        if (sortByRating) {
            events.sort(RATING_ORDER);
        }

        if (onlyAvailable) {
            events = filterAvailableEvents(events);
        }

        if (requiresInMemoryPagination) {
            return paginate(
                    events,
                    userEventParam.getFrom(),
                    userEventParam.getSize()
            );
        }

        return events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getRecommendations(
            Long userId
    ) {
        List<RecommendedEvent> recommendations =
                analyzerClient.getRecommendationsForUser(
                        userId,
                        DEFAULT_RECOMMENDATION_LIMIT
                );

        if (recommendations == null
                || recommendations.isEmpty()) {

            return Collections.emptyList();
        }

        List<Long> recommendedEventIds =
                recommendations.stream()
                        .filter(Objects::nonNull)
                        .map(RecommendedEvent::eventId)
                        .filter(eventId -> eventId > 0)
                        .distinct()
                        .toList();

        if (recommendedEventIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Event> publishedEventsById =
                eventRepository.findAllByIdIn(
                                recommendedEventIds
                        )
                        .stream()
                        .filter(event ->
                                event.getState()
                                        == EventState.PUBLISHED
                        )
                        .collect(
                                Collectors.toMap(
                                        Event::getId,
                                        event -> event,
                                        (first, second) -> first
                                )
                        );

        if (publishedEventsById.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventShortDto> result =
                recommendedEventIds.stream()
                        .map(publishedEventsById::get)
                        .filter(Objects::nonNull)
                        .map(event ->
                                EventMapper.toEventShortDto(
                                        event,
                                        0L,
                                        0.0
                                )
                        )
                        .collect(Collectors.toList());

        enrichEventsWithRatings(result);
        enrichEventsListWithCounts(result);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public void likeEvent(
            Long userId,
            Long eventId
    ) {
        Event event =
                eventRepository.findById(eventId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Event with id="
                                                + eventId
                                                + " was not found"
                                )
                        );

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException(
                    "Published event with id="
                            + eventId
                            + " was not found"
            );
        }

        boolean hasConfirmedRequest =
                requestCountProvider.hasConfirmedRequest(
                        userId,
                        eventId
                );

        if (!hasConfirmedRequest) {
            throw new BadRequestException(
                    "Only users with a confirmed "
                            + "participation request can like "
                            + "event with id="
                            + eventId
            );
        }

        collectorClient.sendAction(
                userId,
                eventId,
                UserActionType.LIKE
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getEventsForAdminRequests(
            AdminUserEventParam adminParam
    ) {
        EventRepositoryParam param =
                EventRepositoryParam.fromAdminEventParam(
                        adminParam
                );

        List<EventFullDto> events =
                eventRepository.findEventsFullDto(param);

        if (events.isEmpty()) {
            return events;
        }

        enrichEventsWithRatings(events);
        enrichEventsListWithCounts(events);

        return events;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto findUserEventByEventId(
            Long userId,
            Long eventId
    ) {
        EventFullDto event =
                eventRepository.findEventByIdFullDto(eventId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Event with id="
                                                + eventId
                                                + " was not found"
                                )
                        );

        if (event.getInitiator() == null
                || event.getInitiator().getId() == null
                || !event.getInitiator()
                .getId()
                .equals(userId)) {

            throw new NotFoundException(
                    "Event with id="
                            + eventId
                            + " not found for user with id="
                            + userId
            );
        }

        enrichEventWithRating(event);
        enrichEventsListWithCounts(List.of(event));

        return event;
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(
            Long userId,
            Long eventId,
            UpdateEventUserRequest body
    ) {
        Event event =
                eventRepository.findById(eventId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Event with id="
                                                + eventId
                                                + " was not found"
                                )
                        );

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException(
                    "Event with id="
                            + eventId
                            + " not found for user with id="
                            + userId
            );
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConditionsNotMetException(
                    "Only events with CANCELED or PENDING "
                            + "state can be updated"
            );
        }

        LocalDateTime minEventDateForUpdating =
                LocalDateTime.now().plusHours(2);

        if (event.getEventDate().isBefore(
                minEventDateForUpdating
        )) {
            throw new ConditionsNotMetException(
                    "Unable to update event less than "
                            + "2 hours before event date"
            );
        }

        if (body.getEventDate() != null
                && body.getEventDate().isBefore(
                minEventDateForUpdating
        )) {
            throw new ConditionsNotMetException(
                    "Unable to set event date less than "
                            + "2 hours from now"
            );
        }

        if (body.getStateAction() != null) {
            switch (body.getStateAction()) {
                case SEND_TO_REVIEW -> {
                    if (event.getState()
                            == EventState.CANCELED) {

                        event.setState(
                                EventState.PENDING
                        );
                    }
                }

                case CANCEL_REVIEW -> {
                    if (event.getState()
                            != EventState.PENDING) {

                        throw new ConditionsNotMetException(
                                "Only events in PENDING "
                                        + "state can be cancelled"
                        );
                    }

                    event.setState(
                            EventState.CANCELED
                    );
                }

                default -> throw new ConditionsNotMetException(
                        "Unknown state action: "
                                + body.getStateAction()
                );
            }
        }

        Category category = null;

        if (body.getCategory() != null) {
            category =
                    categoryRepository.getCategory(
                            body.getCategory()
                    );
        }

        EventMapper.updateEventFromUserRequest(
                body,
                event,
                category
        );

        event = eventRepository.save(event);

        double rating =
                fetchRatings(List.of(event.getId()))
                        .getOrDefault(
                                event.getId(),
                                0.0
                        );

        long confirmedRequests =
                requestCountProvider
                        .getConfirmedRequests(eventId);

        EventFullDto eventFullDto =
                EventMapper.toEventFullDto(
                        event,
                        confirmedRequests,
                        rating
                );

        enrichEventsListWithCounts(
                List.of(eventFullDto)
        );

        return eventFullDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(
            Long eventId,
            UpdateEventAdminRequest body
    ) {
        Event event =
                eventRepository.findById(eventId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Event with id="
                                                + eventId
                                                + " was not found"
                                )
                        );

        Category category = null;

        if (body.getCategory() != null) {
            category =
                    categoryRepository.getCategory(
                            body.getCategory()
                    );
        }

        EventMapper.updateEventFromAdminRequest(
                body,
                event,
                category
        );

        if (body.getStateAction() != null) {
            switch (body.getStateAction()) {
                case PUBLISH_EVENT -> {
                    if (event.getState()
                            != EventState.PENDING) {

                        throw new ConditionsNotMetException(
                                "Cannot publish the event because "
                                        + "it is not in the right state: "
                                        + event.getState()
                        );
                    }

                    LocalDateTime minPublishDate =
                            LocalDateTime.now()
                                    .plusHours(1);

                    if (event.getEventDate().isBefore(
                            minPublishDate
                    )) {
                        throw new ConditionsNotMetException(
                                "Event date must be at least "
                                        + "1 hour from publication time"
                        );
                    }

                    event.setState(
                            EventState.PUBLISHED
                    );

                    event.setPublishedOn(
                            LocalDateTime.now()
                    );
                }

                case REJECT_EVENT -> {
                    if (event.getState()
                            == EventState.PUBLISHED) {

                        throw new ConditionsNotMetException(
                                "Cannot reject a published event"
                        );
                    }

                    event.setState(
                            EventState.CANCELED
                    );
                }

                default -> throw new ConditionsNotMetException(
                        "Unknown state action: "
                                + body.getStateAction()
                );
            }
        }

        event = eventRepository.save(event);

        double rating =
                fetchRatings(List.of(event.getId()))
                        .getOrDefault(
                                event.getId(),
                                0.0
                        );

        long confirmedRequests =
                requestCountProvider
                        .getConfirmedRequests(eventId);

        EventFullDto eventFullDto =
                EventMapper.toEventFullDto(
                        event,
                        confirmedRequests,
                        rating
                );

        enrichEventsListWithCounts(
                List.of(eventFullDto)
        );

        return eventFullDto;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto findEventById(
            String uri,
            String ip,
            long id,
            long userId
    ) {
        EventFullDto event =
                eventRepository.findEventByIdFullDto(id)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Event with id="
                                                + id
                                                + " was not found"
                                )
                        );

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException(
                    "Published event with id="
                            + id
                            + " was not found"
            );
        }

        enrichEventWithRating(event);
        enrichEventsListWithCounts(
                List.of(event)
        );

        collectorClient.sendAction(
                userId,
                id,
                UserActionType.VIEW
        );

        return event;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getShortDtosByIds(
            Collection<Long> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Event> events =
                eventRepository.findAllByIdIn(eventIds);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Event> eventsById =
                events.stream()
                        .collect(
                                Collectors.toMap(
                                        Event::getId,
                                        event -> event
                                )
                        );

        List<EventShortDto> eventDtos =
                eventIds.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .map(eventsById::get)
                        .filter(Objects::nonNull)
                        .map(event ->
                                EventMapper.toEventShortDto(
                                        event,
                                        0L,
                                        0.0
                                )
                        )
                        .collect(Collectors.toList());

        enrichEventsWithRatings(eventDtos);
        enrichEventsListWithCounts(eventDtos);

        return eventDtos;
    }

    private <T extends Rateable>
    void enrichEventsWithRatings(
            List<T> events
    ) {
        if (events == null || events.isEmpty()) {
            return;
        }

        List<Long> eventIds =
                events.stream()
                        .map(Rateable::getId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        Map<Long, Double> ratings =
                fetchRatings(eventIds);

        events.forEach(event ->
                event.setRating(
                        ratings.getOrDefault(
                                event.getId(),
                                0.0
                        )
                )
        );
    }

    private void enrichEventWithRating(
            EventFullDto event
    ) {
        if (event == null
                || event.getId() == null) {

            return;
        }

        Map<Long, Double> ratings =
                fetchRatings(
                        List.of(event.getId())
                );

        event.setRating(
                ratings.getOrDefault(
                        event.getId(),
                        0.0
                )
        );
    }

    private Map<Long, Double> fetchRatings(
            Collection<Long> eventIds
    ) {
        if (eventIds == null
                || eventIds.isEmpty()) {

            return Collections.emptyMap();
        }

        try {
            List<RecommendedEvent> ratings =
                    analyzerClient
                            .getInteractionsCount(
                                    eventIds
                            );

            if (ratings == null
                    || ratings.isEmpty()) {

                return Collections.emptyMap();
            }

            return ratings.stream()
                    .filter(Objects::nonNull)
                    .collect(
                            Collectors.toMap(
                                    RecommendedEvent::eventId,
                                    RecommendedEvent::score,
                                    Double::max
                            )
                    );

        } catch (Exception exception) {
            log.error(
                    "Failed to fetch ratings "
                            + "from analyzer: eventIds={}",
                    eventIds,
                    exception
            );

            return Collections.emptyMap();
        }
    }

    private <T extends EventCountsAware>
    void enrichEventsListWithCounts(
            List<T> eventDtos
    ) {
        if (eventDtos == null
                || eventDtos.isEmpty()) {

            return;
        }

        requestCountProvider.enrich(eventDtos);
        commentCountProvider.enrich(eventDtos);
    }

    private List<EventShortDto>
    filterAvailableEvents(
            List<EventShortDto> events
    ) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds =
                events.stream()
                        .map(EventShortDto::getId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        Map<Long, Integer> participantLimits =
                eventRepository.findAllByIdIn(eventIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Event::getId,
                                        event ->
                                                event.getParticipantLimit()
                                                        == null
                                                        ? 0
                                                        : event
                                                        .getParticipantLimit()
                                )
                        );

        return events.stream()
                .filter(eventDto -> {
                    int participantLimit =
                            participantLimits
                                    .getOrDefault(
                                            eventDto.getId(),
                                            0
                                    );

                    long confirmedRequests =
                            eventDto.getConfirmedRequests()
                                    == null
                                    ? 0L
                                    : eventDto
                                    .getConfirmedRequests();

                    return participantLimit == 0
                            || confirmedRequests
                            < participantLimit;
                })
                .collect(Collectors.toList());
    }

    private <T> List<T> paginate(
            List<T> values,
            int from,
            int size
    ) {
        if (values == null
                || values.isEmpty()
                || size <= 0
                || from >= values.size()) {

            return Collections.emptyList();
        }

        int safeFrom = Math.max(from, 0);

        long requestedEnd =
                (long) safeFrom + size;

        int toIndex =
                (int) Math.min(
                        values.size(),
                        requestedEnd
                );

        return List.copyOf(
                values.subList(
                        safeFrom,
                        toIndex
                )
        );
    }
}