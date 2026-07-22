package ru.practicum.ewm.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;

import java.util.Collection;
import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    long countByEventIdAndStatus(
            Long eventId,
            RequestStatus status
    );

    boolean existsByEventIdAndRequesterId(
            Long eventId,
            Long requesterId
    );

    boolean existsByEventIdAndRequesterIdAndStatus(
            Long eventId,
            Long requesterId,
            RequestStatus status
    );

    List<ParticipationRequest> findByRequesterId(
            Long requesterId
    );

    List<ParticipationRequest> findByEventId(
            Long eventId
    );

    List<ParticipationRequest> findByIdIn(
            Collection<Long> requestIds
    );

    @Modifying
    @Query("""
            UPDATE ParticipationRequest request
            SET request.status = :status
            WHERE request.id IN :ids
            """)
    int updateStatusByIdIn(
            @Param("ids") Collection<Long> ids,
            @Param("status") RequestStatus status
    );

    @Modifying
    @Query("""
            UPDATE ParticipationRequest request
            SET request.status = :newStatus
            WHERE request.eventId = :eventId
              AND request.status = :oldStatus
            """)
    int updateStatusByEventId(
            @Param("eventId") Long eventId,
            @Param("oldStatus") RequestStatus oldStatus,
            @Param("newStatus") RequestStatus newStatus
    );

    @Query("""
            SELECT request.eventId, COUNT(request.id)
            FROM ParticipationRequest request
            WHERE request.eventId IN :eventIds
              AND request.status = :status
            GROUP BY request.eventId
            """)
    List<Object[]> countByEventIdsAndStatus(
            @Param("eventIds") Collection<Long> eventIds,
            @Param("status") RequestStatus status
    );
}