package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.analyzer.model.UserActionEntity;
import ru.practicum.ewm.stats.analyzer.model.UserActionId;

import java.util.Collection;
import java.util.List;

public interface UserActionRepository extends JpaRepository<UserActionEntity, UserActionId> {

    @Query("""
            select action
            from UserActionEntity action
            where action.id.userId = :userId
            order by action.lastInteractionAt desc
            """)
    List<UserActionEntity>
    findAllByUserIdOrderByInteractionTimeDesc(
            @Param("userId") Long userId
    );

    @Query("""
            select action.id.eventId
            from UserActionEntity action
            where action.id.userId = :userId
            """)
    List<Long> findEventIdsByUserId(
            @Param("userId") Long userId
    );

    @Query("""
            select action.id.eventId as eventId,
                   sum(action.rating) as score
            from UserActionEntity action
            where action.id.eventId in :eventIds
            group by action.id.eventId
            """)
    List<EventInteractionScoreProjection>
    sumRatingsByEventIds(
            @Param("eventIds")
            Collection<Long> eventIds
    );

    interface EventInteractionScoreProjection {

        Long getEventId();

        Double getScore();
    }
}