package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityId;

import java.util.Collection;
import java.util.List;

public interface EventSimilarityRepository extends
        JpaRepository<
                EventSimilarityEntity,
                EventSimilarityId
                > {

    @Query("""
            select similarity
            from EventSimilarityEntity similarity
            where similarity.id.eventA in :eventIds
               or similarity.id.eventB in :eventIds
            order by similarity.score desc
            """)
    List<EventSimilarityEntity>
    findAllConnectedToAnyEvent(
            @Param("eventIds")
            Collection<Long> eventIds
    );

    @Query("""
            select similarity
            from EventSimilarityEntity similarity
            where similarity.id.eventA = :eventId
               or similarity.id.eventB = :eventId
            order by similarity.score desc
            """)
    List<EventSimilarityEntity>
    findAllConnectedToEvent(
            @Param("eventId") Long eventId
    );
}