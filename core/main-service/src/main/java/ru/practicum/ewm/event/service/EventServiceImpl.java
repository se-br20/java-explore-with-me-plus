package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.categories.model.Category;
import ru.practicum.ewm.categories.repository.CategoryRepository;
import ru.practicum.ewm.comments.model.CommentStatus;
import ru.practicum.ewm.comments.repository.CommentRepository;
import ru.practicum.ewm.event.dto.Commentable;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventMapper;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.dto.Viewable;
import ru.practicum.ewm.event.dto.paramDto.AdminUserEventParam;
import ru.practicum.ewm.event.dto.paramDto.EventRepositoryParam;
import ru.practicum.ewm.event.dto.paramDto.PublicUserEventParam;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventSort;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.exceptions.ConditionsNotMetException;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.dto.ParticipationRequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.stat.client.StatsClient;
import ru.practicum.stat.dto.EndpointHitDto;
import ru.practicum.stat.dto.ParamDto;
import ru.practicum.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final LocalDateTime STATS_DEFAULT_START =
            LocalDateTime.of(1970, 1, 1, 0, 0);

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public EventFullDto createEvent(
            Long userId,
            NewEventDto newEventDto
    ) {
        LocalDateTime minEventDate =
                LocalDateTime.now().plusHours(2);

        if (newEventDto.getEventDate().isBefore(minEventDate)) {
            throw new ConditionsNotMetException(
                    "Event date must be at least 2 hours from now"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "User with id=" + userId + " was not found"
                ));

        Category category =
                categoryRepository.getCategory(
                        newEventDto.getCategory()
                );

        Event event = EventMapper.toEvent(
                newEventDto,
                category,
                user
        );

        event = eventRepository.save(event);

        EventFullDto eventDto =
                EventMapper.toEventFullDto(
                        event,
                        0L,
                        0L
                );

        enrichEventsListWithCommentsCount(
                List.of(eventDto)
        );

        return eventDto;
    }

    @Override
    public List<EventShortDto> getUserEvents(
            Long userId,
            int from,
            int size
    ) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(
                    "User with id=" + userId + " was not found"
            );
        }

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

        List<EventShortDto> events =
                eventRepository.findEventsShortDto(param);

        if (events.isEmpty()) {
            sendHit(
                    userEventParam.getUri(),
                    userEventParam.getIp(),
                    LocalDateTime.now()
            );

            return events;
        }

        enrichEventsWithViews(events);
        enrichEventsListWithCommentsCount(events);

        if (param.getSortOrDefault() == EventSort.VIEWS) {
            events.sort(
                    Comparator.comparing(
                            EventShortDto::getViews
                    ).reversed()
            );
        }

        sendHit(
                userEventParam.getUri(),
                userEventParam.getIp(),
                LocalDateTime.now()
        );

        return events;
    }

    @Override
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

        enrichEventsWithViews(events);
        enrichEventsListWithCommentsCount(events);

        return events;
    }

    @Override
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

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(
                    "Event with id=" + eventId
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
    public EventFullDto updateUserEvent(
            Long userId,
            Long eventId,
            UpdateEventUserRequest body
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        "Event with id=" + eventId
                                + " was not found"
                ));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(
                    "Event with id=" + eventId
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

        LocalDateTime minEventDate =
                LocalDateTime.now().plusHours(2);

        if (event.getEventDate().isBefore(minEventDate)) {
            throw new ConditionsNotMetException(
                    "Unable to update event less than "
                            + "2 hours before event date"
            );
        }

        if (body.getEventDate() != null
                && body.getEventDate().isBefore(minEventDate)) {
            throw new ConditionsNotMetException(
                    "New event date must be at least "
                            + "2 hours from now"
            );
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
                        throw new ConditionsNotMetException(
                                "Only events in PENDING state "
                                        + "can be cancelled"
                        );
                    }

                    event.setState(EventState.CANCELED);
                    break;

                default:
                    throw new ConditionsNotMetException(
                            "Unknown state action: "
                                    + body.getStateAction()
                    );
            }
        }

        Category category = null;

        if (body.getCategory() != null) {
            category = categoryRepository.getCategory(
                    body.getCategory()
            );
        }

        EventMapper.updateEventFromUserRequest(
                body,
                event,
                category
        );

        event = eventRepository.save(event);

        Long views = fetchViewsForEvent(event);

        long confirmedRequests =
                requestRepository.countByEventIdAndStatus(
                        eventId,
                        RequestStatus.CONFIRMED
                );

        EventFullDto eventDto =
                EventMapper.toEventFullDto(
                        event,
                        confirmedRequests,
                        views
                );

        enrichEventsListWithCommentsCount(
                List.of(eventDto)
        );

        return eventDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(
            Long eventId,
            UpdateEventAdminRequest body
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        "Event with id=" + eventId
                                + " was not found"
                ));

        Category category = null;

        if (body.getCategory() != null) {
            category = categoryRepository.getCategory(
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
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConditionsNotMetException(
                                "Cannot publish the event because "
                                        + "it is not in PENDING state"
                        );
                    }

                    LocalDateTime minPublishDate =
                            LocalDateTime.now().plusHours(1);

                    if (event.getEventDate()
                            .isBefore(minPublishDate)) {
                        throw new ConditionsNotMetException(
                                "Event date must be at least "
                                        + "1 hour from publication"
                        );
                    }

                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;

                case REJECT_EVENT:
                    if (event.getState()
                            == EventState.PUBLISHED) {
                        throw new ConditionsNotMetException(
                                "Cannot reject a published event"
                        );
                    }

                    event.setState(EventState.CANCELED);
                    break;

                default:
                    throw new ConditionsNotMetException(
                            "Unknown state action: "
                                    + body.getStateAction()
                    );
            }
        }

        event = eventRepository.save(event);

        Long views = fetchViewsForEvent(event);

        long confirmedRequests =
                requestRepository.countByEventIdAndStatus(
                        eventId,
                        RequestStatus.CONFIRMED
                );

        EventFullDto eventDto =
                EventMapper.toEventFullDto(
                        event,
                        confirmedRequests,
                        views
                );

        enrichEventsListWithCommentsCount(
                List.of(eventDto)
        );

        return eventDto;
    }

    @Override
    public EventFullDto findEventById(
            String uri,
            String ip,
            Long id
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

        sendHit(uri, ip, LocalDateTime.now());

        enrichEventWithViews(event);
        enrichEventsListWithCommentsCount(
                List.of(event)
        );

        return event;
    }

    @Override
    public List<ParticipationRequestDto>
    getParticipationRequests(
            Long userId,
            Long eventId
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        "Event with id=" + eventId
                                + " was not found"
                ));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(
                    "Event with id=" + eventId
                            + " not found for user with id="
                            + userId
            );
        }

        List<ParticipationRequest> requests =
                requestRepository.findByEventId(eventId);

        return ParticipationRequestMapper
                .toParticipationRequestDto(requests);
    }

    /*
     * Именно этот метод отвечает за подтверждение
     * и отклонение заявок на участие.
     */
    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatuses(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest updateRequest
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        "Event with id=" + eventId
                                + " was not found"
                ));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(
                    "Event with id=" + eventId
                            + " not found for user with id="
                            + userId
            );
        }

        if (updateRequest.getStatus()
                != RequestStatus.CONFIRMED
                && updateRequest.getStatus()
                != RequestStatus.REJECTED) {
            throw new ConditionsNotMetException(
                    "Request status must be "
                            + "CONFIRMED or REJECTED"
            );
        }

        List<Long> distinctIds =
                updateRequest.getRequestIds()
                        .stream()
                        .distinct()
                        .toList();

        if (distinctIds.size()
                != updateRequest.getRequestIds().size()) {
            throw new ConditionsNotMetException(
                    "Request identifiers must not "
                            + "contain duplicates"
            );
        }

        List<ParticipationRequest> requests =
                requestRepository.findByIdIn(distinctIds);

        if (requests.size() != distinctIds.size()) {
            throw new NotFoundException(
                    "One or more participation "
                            + "requests were not found"
            );
        }

        for (ParticipationRequest request : requests) {
            if (!request.getEvent()
                    .getId()
                    .equals(eventId)) {
                throw new ConditionsNotMetException(
                        "Request with id="
                                + request.getId()
                                + " does not belong to event "
                                + "with id=" + eventId
                );
            }

            if (request.getStatus()
                    != RequestStatus.PENDING) {
                throw new ConditionsNotMetException(
                        "Only requests with PENDING "
                                + "status can be reviewed"
                );
            }
        }

        List<ParticipationRequest> confirmed =
                new ArrayList<>();

        List<ParticipationRequest> rejected =
                new ArrayList<>();

        if (updateRequest.getStatus()
                == RequestStatus.REJECTED) {
            rejected.addAll(requests);
        } else {
            long limit = event.getParticipantLimit();

            if (limit == 0
                    || !event.getRequestModeration()) {
                confirmed.addAll(requests);
            } else {
                long alreadyConfirmed =
                        requestRepository
                                .countByEventIdAndStatus(
                                        eventId,
                                        RequestStatus.CONFIRMED
                                );

                if (alreadyConfirmed >= limit) {
                    throw new ConditionsNotMetException(
                            "The participant limit "
                                    + "has been reached"
                    );
                }

                long availablePlaces =
                        limit - alreadyConfirmed;

                for (ParticipationRequest request : requests) {
                    if (availablePlaces > 0) {
                        confirmed.add(request);
                        availablePlaces--;
                    } else {
                        rejected.add(request);
                    }
                }
            }
        }

        updateStatuses(
                confirmed,
                RequestStatus.CONFIRMED
        );

        updateStatuses(
                rejected,
                RequestStatus.REJECTED
        );

        long limit = event.getParticipantLimit();

        if (limit > 0) {
            long confirmedCount =
                    requestRepository
                            .countByEventIdAndStatus(
                                    eventId,
                                    RequestStatus.CONFIRMED
                            );

            /*
             * Если лимит исчерпан, отклоняем все
             * остальные ожидающие заявки.
             */
            if (confirmedCount >= limit) {
                requestRepository.updateStatusByEventId(
                        eventId,
                        RequestStatus.PENDING,
                        RequestStatus.REJECTED
                );
            }
        }

        return ParticipationRequestMapper
                .toEventRequestStatusUpdateResult(
                        confirmed,
                        rejected
                );
    }

    /*
     * Этот метод используется сервисом подборок.
     *
     * Он получает EventShortDto сразу для набора id.
     */
    @Override
    public List<EventShortDto> getShortDtosByIds(
            Collection<Long> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = eventIds.stream()
                .distinct()
                .toList();

        EventRepositoryParam param =
                EventRepositoryParam.builder()
                        .ids(ids)
                        .from(0)
                        .size(ids.size())
                        .build();

        /*
         * Здесь используется QueryDSL-проекция,
         * которая сразу считает confirmedRequests.
         *
         * Поэтому больше не устанавливаем
         * confirmedRequests = 0 вручную.
         */
        List<EventShortDto> events =
                eventRepository.findEventsShortDto(param);

        enrichEventsWithViews(events);
        enrichEventsListWithCommentsCount(events);

        return events;
    }

    private void updateStatuses(
            List<ParticipationRequest> requests,
            RequestStatus status
    ) {
        if (requests.isEmpty()) {
            return;
        }

        List<Long> ids = requests.stream()
                .map(ParticipationRequest::getId)
                .toList();

        int updated =
                requestRepository.updateStatusByIdIn(
                        ids,
                        status
                );

        if (updated != ids.size()) {
            throw new IllegalStateException(
                    String.format(
                            "Failed to update all requests. "
                                    + "Expected: %d, updated: %d",
                            ids.size(),
                            updated
                    )
            );
        }

        requests.forEach(
                request -> request.setStatus(status)
        );
    }

    private <T extends Viewable> void enrichEventsWithViews(
            List<T> events
    ) {
        if (events == null || events.isEmpty()) {
            return;
        }

        LocalDateTime minPublishedOn =
                events.stream()
                        .map(Viewable::getPublishedOn)
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo)
                        .orElse(STATS_DEFAULT_START);

        String[] uris = events.stream()
                .map(event ->
                        "/events/" + event.getId()
                )
                .toArray(String[]::new);

        Map<Long, Long> hits =
                fetchViews(uris, minPublishedOn);

        events.forEach(event ->
                event.setViews(
                        hits.getOrDefault(
                                event.getId(),
                                0L
                        )
                )
        );
    }

    private Long fetchViewsForEvent(Event event) {
        String[] uris = {
                "/events/" + event.getId()
        };

        LocalDateTime start =
                event.getPublishedOn() != null
                        ? event.getPublishedOn()
                        : STATS_DEFAULT_START;

        Map<Long, Long> hits =
                fetchViews(uris, start);

        return hits.getOrDefault(
                event.getId(),
                0L
        );
    }

    private Map<Long, Long> fetchViews(
            String[] uris,
            LocalDateTime date
    ) {
        if (uris == null || uris.length == 0) {
            return Collections.emptyMap();
        }

        LocalDateTime start =
                date != null
                        ? date
                        : STATS_DEFAULT_START;

        ParamDto param = ParamDto.builder()
                .start(start)
                .end(LocalDateTime.now().plusSeconds(1))
                .uris(uris)
                .unique(true)
                .build();

        log.debug(
                "Fetching views for uris={}, params={}",
                Arrays.toString(uris),
                param
        );

        try {
            List<ViewStatsDto> stats =
                    statsClient.get(param);

            if (stats == null || stats.isEmpty()) {
                return Collections.emptyMap();
            }

            return stats.stream()
                    .filter(stat -> stat.getUri() != null)
                    .filter(stat -> stat.getHits() != null)
                    .filter(stat -> stat.getHits() >= 0)
                    .collect(
                            Collectors.toMap(
                                    this::extractEventIdFromUri,
                                    ViewStatsDto::getHits,
                                    Long::sum
                            )
                    );
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to fetch views: {}",
                    exception.getMessage()
            );

            return Collections.emptyMap();
        }
    }

    private void enrichEventWithViews(
            EventFullDto event
    ) {
        String[] uris = {
                "/events/" + event.getId()
        };

        LocalDateTime start =
                event.getPublishedOn() != null
                        ? event.getPublishedOn()
                        : STATS_DEFAULT_START;

        Map<Long, Long> hits =
                fetchViews(uris, start);

        event.setViews(
                hits.getOrDefault(
                        event.getId(),
                        0L
                )
        );
    }

    private Long extractEventIdFromUri(
            ViewStatsDto stat
    ) {
        String uri = stat.getUri();

        return Long.parseLong(
                uri.substring(
                        uri.lastIndexOf('/') + 1
                )
        );
    }

    private void sendHit(
            String uri,
            String ip,
            LocalDateTime time
    ) {
        EndpointHitDto hitDto =
                EndpointHitDto.builder()
                        .uri(uri)
                        .ip(ip)
                        .timestamp(time)
                        .build();

        statsClient.hit(hitDto);
    }

    /*
     * Здесь одним SQL-запросом получаем количество
     * APPROVED-комментариев для всех событий.
     */
    private <T extends Commentable>
    void enrichEventsListWithCommentsCount(
            List<T> eventDtos
    ) {
        if (eventDtos == null || eventDtos.isEmpty()) {
            return;
        }

        List<Long> eventIds = eventDtos.stream()
                .map(Commentable::getId)
                .distinct()
                .toList();

        List<Object[]> counts =
                commentRepository
                        .countByEventIdInAndStatus(
                                eventIds,
                                CommentStatus.APPROVED
                        );

        /*
         * row[0] — eventId.
         * row[1] — COUNT(c).
         *
         * Приведение через Number работает и с
         * PostgreSQL, и с H2.
         */
        Map<Long, Long> countsMap =
                counts.stream()
                        .collect(
                                Collectors.toMap(
                                        row ->
                                                ((Number) row[0])
                                                        .longValue(),
                                        row ->
                                                ((Number) row[1])
                                                        .longValue()
                                )
                        );

        eventDtos.forEach(event ->
                event.setCommentsCount(
                        countsMap.getOrDefault(
                                event.getId(),
                                0L
                        )
                )
        );
    }
}