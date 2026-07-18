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
import ru.practicum.ewm.exceptions.exceptions.ConditionsNotMetException;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.ewm.interaction.CommentCountProvider;
import ru.practicum.ewm.interaction.RequestCountProvider;
import ru.practicum.ewm.interaction.client.UserServiceClient;
import ru.practicum.interaction.user.UserDetailsDto;
import ru.practicum.stat.client.StatsClient;
import ru.practicum.stat.dto.EndpointHitDto;
import ru.practicum.stat.dto.ParamDto;
import ru.practicum.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserServiceClient userServiceClient;
    private final CategoryRepository categoryRepository;
    private final StatsClient statsClient;
    private final CommentCountProvider commentCountProvider;
    private final RequestCountProvider requestCountProvider;


    @Transactional
    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        LocalDateTime minEventDate = LocalDateTime.now().plusHours(2);

        if (newEventDto.getEventDate().isBefore(minEventDate)) {
            throw new ConditionsNotMetException(
                    "Unable to update event at last 2 hours before event date"
            );
        }

        UserDetailsDto user =
                userServiceClient.getUser(userId);

        Category cat = categoryRepository.getCategory(newEventDto.getCategory());

        Event event = EventMapper.toEvent(newEventDto, cat, user);

        event = eventRepository.save(event);

        return EventMapper.toEventFullDto(event, 0L, 0L);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        EventRepositoryParam param = EventRepositoryParam.builder()
                .users(List.of(userId))
                .from(from)
                .size(size)
                .build();

        List<EventShortDto> events = eventRepository.findEventsShortDto(param);
        if (events.isEmpty()) {
            return events;
        }

        enrichEventsWithViews(events);
        enrichEventsListWithCommentsCount(events);

        return events;
    }

    @Override
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
        List<EventShortDto> events = onlyAvailable
                ? eventRepository
                .findEventsShortDtoWithoutPagination(param)
                : eventRepository.findEventsShortDto(param);

        sendHit(
                userEventParam.getUri(),
                userEventParam.getIp(),
                LocalDateTime.now()
        );

        if (events.isEmpty()) {
            return events;
        }

        enrichEventsWithViews(events);
        enrichEventsListWithCommentsCount(events);

        if (param.getSortOrDefault() == EventSort.VIEWS) {
            events.sort(
                    Comparator.comparing(
                                    EventShortDto::getViews,
                                    Comparator.nullsFirst(
                                            Comparator.naturalOrder()
                                    )
                            )
                            .reversed()
            );
        }

        if (onlyAvailable) {
            events = filterAndPaginateAvailableEvents(
                    events,
                    userEventParam.getFrom(),
                    userEventParam.getSize()
            );
        }

        return events;
    }

    @Override
    public List<EventFullDto> getEventsForAdminRequests(AdminUserEventParam adminParam) {
        EventRepositoryParam param = EventRepositoryParam.fromAdminEventParam(adminParam);

        List<EventFullDto> events = eventRepository.findEventsFullDto(param);
        if (events.isEmpty()) {
            return events;
        }

        enrichEventsWithViews(events);
        enrichEventsListWithCommentsCount(events);

        return events;
    }

    @Override
    public EventFullDto findUserEventByEventId(
            Long userId,
            Long eventId
    ) {
        EventFullDto event = eventRepository
                .findEventByIdFullDto(eventId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Event with id="
                                        + eventId
                                        + " was not found"
                        )
                );

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(
                    "Event with id="
                            + eventId
                            + " not found for user with id="
                            + userId
            );
        }

        enrichEventWithViews(event);
        enrichEventsListWithCommentsCount(
                List.of(event)
        );

        return event;
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest body) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " not found for user with id=" + userId);
        }

        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConditionsNotMetException("Only events with CANCELED or PENDING state can be updated");
        }

        LocalDateTime minEventDateForUpdating = LocalDateTime.now().plusHours(2);
        if (event.getEventDate().isBefore(minEventDateForUpdating)) {
            throw new ConditionsNotMetException("Unable to update event at last 2 hours before event date");
        }

        if (body.getEventDate() != null) {
            LocalDateTime newEventDate = body.getEventDate();
            if (newEventDate.isBefore(minEventDateForUpdating)) {
                throw new ConditionsNotMetException("Unable to update event at last 2 hours before event date");
            }

        }

        if (body.getStateAction() != null) {
            switch (body.getStateAction()) {
                case SEND_TO_REVIEW:
                    if (event.getState() == EventState.CANCELED) {
                        event.setState(EventState.PENDING);
                    }
                    break;
                case CANCEL_REVIEW:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConditionsNotMetException("Only events in PENDING state can be cancelled");
                    }
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new ConditionsNotMetException("Unknown state action: " + body.getStateAction());
            }
        }

        Category cat = null;
        if (body.getCategory() != null) {
            cat = categoryRepository.getCategory(body.getCategory());
        }
        EventMapper.updateEventFromUserRequest(body, event, cat);

        event = eventRepository.save(event);

        String[] uris = {"/events/" + event.getId()};
        Map<Long, Long> hits = fetchViews(uris, event.getEventDate());
        Long views = hits.getOrDefault(event.getId(), 0L);

        long confirmedRequests =
                requestCountProvider.getConfirmedRequests(eventId);

        EventFullDto eventFullDto = EventMapper.toEventFullDto(event, confirmedRequests, views);
        enrichEventsListWithCommentsCount(List.of(eventFullDto));

        return eventFullDto;
    }


    @Override
    @Transactional
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest body) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        Category category = null;
        if (body.getCategory() != null) {
            category = categoryRepository.getCategory(body.getCategory());
        }

        EventMapper.updateEventFromAdminRequest(body, event, category);

        if (body.getStateAction() != null) {
            switch (body.getStateAction()) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        String msg = "Cannot publish the event because it's not in the right state: " + event.getState();
                        throw new ConditionsNotMetException(msg);
                    }
                    LocalDateTime minPublishDate = LocalDateTime.now().plusHours(1);
                    if (event.getEventDate().isBefore(minPublishDate)) {
                        throw new ConditionsNotMetException("Event date must be at least 1 hour from now");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;

                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConditionsNotMetException("Cannot reject published event");
                    }
                    event.setState(EventState.CANCELED);
                    break;

                default:
                    throw new ConditionsNotMetException("Unknown state action: " + body.getStateAction());
            }
        }

        event = eventRepository.save(event);

        String[] uris = {"/events/" + event.getId()};
        Map<Long, Long> hits = fetchViews(uris, event.getEventDate());
        Long views = hits.getOrDefault(event.getId(), 0L);

        long confirmedRequests =
                requestCountProvider.getConfirmedRequests(eventId);

        EventFullDto eventFullDto = EventMapper.toEventFullDto(event, confirmedRequests, views);
        enrichEventsListWithCommentsCount(List.of(eventFullDto));

        return eventFullDto;
    }

    public EventFullDto findEventById(String uri, String ip, Long id) {

        EventFullDto event = eventRepository.findEventByIdFullDto(id)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Published Event with id=" + id + " was not found");
        }

        sendHit(uri, ip, LocalDateTime.now());
        enrichEventWithViews(event);
        enrichEventsListWithCommentsCount(List.of(event));

        return event;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getShortDtosByIds(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Event> events = eventRepository.findAllByIdIn(eventIds);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Event> eventsById = events.stream()
                .collect(Collectors.toMap(
                        Event::getId,
                        event -> event
                ));

        List<EventShortDto> eventDtos = eventIds.stream()
                .distinct()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        0L,
                        0L
                ))
                .collect(Collectors.toList());

        enrichEventsWithViews(eventDtos);
        enrichEventsListWithCommentsCount(eventDtos);

        return eventDtos;
    }

    private <T extends Viewable> void enrichEventsWithViews(List<T> events) {
        LocalDateTime minEventDate = events.stream()
                .map(Viewable::getPublishedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        String[] uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .toArray(String[]::new);

        Map<Long, Long> hits = fetchViews(uris, minEventDate);

        events.forEach(event ->
                event.setViews(hits.getOrDefault(event.getId(), 0L))
        );
    }

    private Map<Long, Long> fetchViews(
            String[] uris,
            LocalDateTime date
    ) {
        LocalDateTime start = date != null
                ? date.truncatedTo(ChronoUnit.SECONDS)
                : LocalDateTime.of(1970, 1, 1, 0, 0);

        LocalDateTime end = LocalDateTime.now()
                .plusSeconds(1)
                .truncatedTo(ChronoUnit.SECONDS);

        ParamDto statRequestParam = ParamDto.builder()
                .start(start)
                .end(end)
                .uris(uris)
                .unique(true)
                .build();

        log.debug(
                "Fetching views: uris={}, start={}, end={}",
                Arrays.toString(uris),
                start,
                end
        );

        try {
            List<ViewStatsDto> stats =
                    statsClient.get(statRequestParam);

            log.debug("Stats received from client: {}", stats);

            return stats.stream()
                    .filter(stat -> stat.getUri() != null)
                    .filter(stat -> stat.getHits() != null)
                    .filter(stat -> stat.getHits() >= 0)
                    .collect(Collectors.toMap(
                            this::extractEventIdFromUri,
                            ViewStatsDto::getHits,
                            Long::sum
                    ));
        } catch (Exception exception) {
            log.error(
                    "Failed to fetch views from stats-server",
                    exception
            );

            return Collections.emptyMap();
        }
    }

    private void enrichEventWithViews(EventFullDto event) {
        String[] uris = {"/events/" + event.getId()};
        Map<Long, Long> hits = fetchViews(uris, event.getPublishedOn());
        event.setViews(hits.getOrDefault(event.getId(), 0L));
    }

    private Long extractEventIdFromUri(ViewStatsDto stat) {
        String uri = stat.getUri();
        return Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
    }

    private void sendHit(
            String uri,
            String ip,
            LocalDateTime time
    ) {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .uri(uri)
                .ip(ip)
                .timestamp(time)
                .build();

        try {
            statsClient.hit(hitDto);
        } catch (Exception exception) {
            log.error(
                    "Failed to send hit to stats-server: uri={}, ip={}",
                    uri,
                    ip,
                    exception
            );
        }
    }

    private <T extends EventCountsAware>
    void enrichEventsListWithCommentsCount(
            List<T> eventDtos
    ) {
        if (eventDtos == null || eventDtos.isEmpty()) {
            return;
        }
        requestCountProvider.enrich(eventDtos);
        commentCountProvider.enrich(eventDtos);
    }

    private List<EventShortDto>
    filterAndPaginateAvailableEvents(
            List<EventShortDto> events,
            int from,
            int size
    ) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = events.stream()
                .map(EventShortDto::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Integer> participantLimits =
                eventRepository.findAllByIdIn(eventIds)
                        .stream()
                        .collect(Collectors.toMap(
                                Event::getId,
                                event -> event.getParticipantLimit() == null
                                        ? 0
                                        : event.getParticipantLimit()
                        ));

        return events.stream()
                .filter(eventDto -> {
                    int participantLimit =
                            participantLimits.getOrDefault(
                                    eventDto.getId(),
                                    0
                            );

                    long confirmedRequests =
                            eventDto.getConfirmedRequests() == null
                                    ? 0L
                                    : eventDto.getConfirmedRequests();

                    return participantLimit == 0
                            || confirmedRequests < participantLimit;
                })
                .skip(from)
                .limit(size)
                .collect(Collectors.toList());
    }
}