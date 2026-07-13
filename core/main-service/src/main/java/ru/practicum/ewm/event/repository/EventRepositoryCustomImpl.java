package ru.practicum.ewm.event.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.categories.dto.CategoryDto;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.paramDto.EventRepositoryParam;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.QEvent;
import ru.practicum.ewm.request.model.QParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventRepositoryCustomImpl implements EventRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QEvent event = QEvent.event;

    private final QParticipationRequest request =
            QParticipationRequest.participationRequest;

    @Override
    public List<EventShortDto> findEventsShortDto(
            EventRepositoryParam param
    ) {
        BooleanBuilder predicate = createPredicate(param);

        JPAQuery<EventShortDto> query = queryFactory
                .select(
                        Projections.constructor(
                                EventShortDto.class,
                                event.id,
                                event.annotation,
                                Projections.constructor(
                                        CategoryDto.class,
                                        event.category.id,
                                        event.category.name
                                ),
                                request.count(),
                                event.eventDate,
                                Projections.constructor(
                                        UserShortDto.class,
                                        event.initiator.id,
                                        event.initiator.name
                                ),
                                event.paid,
                                event.publishedOn,
                                event.title,
                                Expressions.asNumber(0L),
                                Expressions.asNumber(0L)
                        )
                )
                .from(event)
                .leftJoin(request)
                .on(
                        request.event.eq(event)
                                .and(
                                        request.status.eq(
                                                RequestStatus.CONFIRMED
                                        )
                                )
                )
                .where(predicate)
                .groupBy(
                        event.id,
                        event.annotation,
                        event.category.id,
                        event.category.name,
                        event.eventDate,
                        event.initiator.id,
                        event.initiator.name,
                        event.paid,
                        event.participantLimit,
                        event.publishedOn,
                        event.title
                );

        applyOnlyAvailableHaving(query, param);

        return query
                .orderBy(event.eventDate.asc())
                .offset(param.getFrom())
                .limit(param.getSize())
                .fetch();
    }

    @Override
    public Optional<EventFullDto> findEventByIdFullDto(Long id) {
        EventFullDto eventDto = queryFactory
                .select(
                        Projections.constructor(
                                EventFullDto.class,
                                event.id,
                                event.annotation,
                                Projections.constructor(
                                        CategoryDto.class,
                                        event.category.id,
                                        event.category.name
                                ),
                                request.count(),
                                event.createdOn,
                                event.description,
                                event.eventDate,
                                Projections.constructor(
                                        UserShortDto.class,
                                        event.initiator.id,
                                        event.initiator.name
                                ),
                                event.location,
                                event.paid,
                                event.participantLimit,
                                event.publishedOn,
                                event.requestModeration,
                                event.state,
                                event.title,
                                Expressions.asNumber(0L),
                                Expressions.asNumber(0L)
                        )
                )
                .from(event)
                .leftJoin(request)
                .on(
                        request.event.eq(event)
                                .and(
                                        request.status.eq(
                                                RequestStatus.CONFIRMED
                                        )
                                )
                )
                .where(event.id.eq(id))
                .groupBy(
                        event.id,
                        event.annotation,
                        event.category.id,
                        event.category.name,
                        event.createdOn,
                        event.description,
                        event.eventDate,
                        event.initiator.id,
                        event.initiator.name,
                        event.location.lat,
                        event.location.lon,
                        event.paid,
                        event.participantLimit,
                        event.publishedOn,
                        event.requestModeration,
                        event.state,
                        event.title
                )
                .fetchOne();

        return Optional.ofNullable(eventDto);
    }

    @Override
    public List<EventFullDto> findEventsFullDto(
            EventRepositoryParam param
    ) {
        BooleanBuilder predicate = createPredicate(param);

        JPAQuery<EventFullDto> query = queryFactory
                .select(
                        Projections.constructor(
                                EventFullDto.class,
                                event.id,
                                event.annotation,
                                Projections.constructor(
                                        CategoryDto.class,
                                        event.category.id,
                                        event.category.name
                                ),
                                request.count(),
                                event.createdOn,
                                event.description,
                                event.eventDate,
                                Projections.constructor(
                                        UserShortDto.class,
                                        event.initiator.id,
                                        event.initiator.name
                                ),
                                event.location,
                                event.paid,
                                event.participantLimit,
                                event.publishedOn,
                                event.requestModeration,
                                event.state,
                                event.title,
                                Expressions.asNumber(0L),
                                Expressions.asNumber(0L)
                        )
                )
                .from(event)
                .leftJoin(request)
                .on(
                        request.event.eq(event)
                                .and(
                                        request.status.eq(
                                                RequestStatus.CONFIRMED
                                        )
                                )
                )
                .where(predicate)
                .groupBy(
                        event.id,
                        event.annotation,
                        event.category.id,
                        event.category.name,
                        event.createdOn,
                        event.description,
                        event.eventDate,
                        event.initiator.id,
                        event.initiator.name,
                        event.location.lat,
                        event.location.lon,
                        event.paid,
                        event.participantLimit,
                        event.publishedOn,
                        event.requestModeration,
                        event.state,
                        event.title
                );

        applyOnlyAvailableHaving(query, param);

        return query
                .orderBy(event.eventDate.asc())
                .offset(param.getFrom())
                .limit(param.getSize())
                .fetch();
    }

    private void applyOnlyAvailableHaving(
            JPAQuery<?> query,
            EventRepositoryParam param
    ) {
        if (!param.isOnlyAvailable()) {
            return;
        }

        query.having(
                event.participantLimit.eq(0)
                        .or(
                                request.count()
                                        .lt(
                                                event.participantLimit
                                                        .longValue()
                                        )
                        )
        );
    }

    private BooleanBuilder createPredicate(
            EventRepositoryParam param
    ) {
        BooleanBuilder predicate = new BooleanBuilder();

        if (param.isPublicRequest()) {
            predicate.and(
                    event.state.eq(EventState.PUBLISHED)
            );
        } else if (param.hasStates()) {
            predicate.and(
                    event.state.in(param.getStates())
            );
        }

        if (param.hasTextSearchRequest()) {
            predicate.and(
                    event.annotation
                            .containsIgnoreCase(param.getText())
                            .or(
                                    event.description
                                            .containsIgnoreCase(
                                                    param.getText()
                                            )
                            )
            );
        }

        if (param.hasCategories()) {
            predicate.and(
                    event.category.id.in(
                            param.getCategories()
                    )
            );
        }

        if (param.hasPaidParam()) {
            predicate.and(
                    event.paid.eq(param.getPaid())
            );
        }

        if (param.hasUsers()) {
            predicate.and(
                    event.initiator.id.in(
                            param.getUsers()
                    )
            );
        }

        if (param.hasIds()) {
            predicate.and(
                    event.id.in(param.getIds())
            );
        }

        if (param.hasDateRange()) {
            predicate.and(
                    event.eventDate.between(
                            param.getRangeStart(),
                            param.getRangeEnd()
                    )
            );
        } else if (param.hasRangeStart()) {
            predicate.and(
                    event.eventDate.goe(
                            param.getRangeStart()
                    )
            );
        } else if (param.hasRangeEnd()) {
            predicate.and(
                    event.eventDate.loe(
                            param.getRangeEnd()
                    )
            );
        } else if (param.isPublicRequest()) {
            predicate.and(
                    event.eventDate.after(
                            LocalDateTime.now()
                    )
            );
        }

        return predicate;
    }
}