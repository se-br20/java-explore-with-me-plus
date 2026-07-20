package ru.practicum.ewm.comments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comments.client.EventServiceClient;
import ru.practicum.ewm.comments.client.UserServiceClient;
import ru.practicum.ewm.comments.dto.CommentFullDto;
import ru.practicum.ewm.comments.dto.CommentMapper;
import ru.practicum.ewm.comments.dto.CommentShortDto;
import ru.practicum.ewm.comments.dto.NewCommentDto;
import ru.practicum.ewm.comments.dto.UpdateCommentAdminRequest;
import ru.practicum.ewm.comments.dto.UpdateCommentUserRequest;
import ru.practicum.ewm.comments.model.Comment;
import ru.practicum.ewm.comments.model.CommentStatus;
import ru.practicum.ewm.comments.repository.CommentRepository;
import ru.practicum.ewm.exceptions.exceptions.ConditionsNotMetException;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.interaction.comment.ApprovedCommentsResponse;
import ru.practicum.interaction.comment.CommentEventIdsRequest;
import ru.practicum.interaction.event.EventDetailsDto;
import ru.practicum.interaction.event.EventStateDto;
import ru.practicum.interaction.user.UserDetailsDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;

    @Override
    public List<CommentShortDto> getEventComments(
            Long eventId,
            int from,
            int size
    ) {
        PageRequest page = PageRequest.of(
                from / size,
                size
        );

        return commentRepository
                .findByEventIdAndStatusOrderByCreatedDesc(
                        eventId,
                        CommentStatus.APPROVED,
                        page
                )
                .stream()
                .map(CommentMapper::toShortDto)
                .toList();
    }

    @Override
    public CommentShortDto getComment(Long commentId) {
        Comment comment = commentRepository
                .findByIdAndStatus(
                        commentId,
                        CommentStatus.APPROVED
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "Comment with id="
                                        + commentId
                                        + " not found or not approved"
                        )
                );

        return CommentMapper.toShortDto(comment);
    }

    @Override
    @Transactional
    public CommentFullDto createComment(
            Long userId,
            Long eventId,
            NewCommentDto dto
    ) {
        UserDetailsDto author =
                userServiceClient.getUser(userId);

        EventDetailsDto event =
                eventServiceClient.getEvent(eventId);

        if (event.getState() != EventStateDto.PUBLISHED) {
            throw new ConditionsNotMetException(
                    "Cannot comment on unpublished event"
            );
        }

        Comment comment = CommentMapper.toComment(
                dto,
                author,
                event
        );

        return CommentMapper.toFullDto(
                commentRepository.save(comment)
        );
    }

    @Override
    @Transactional
    public CommentFullDto updateCommentByUser(
            Long userId,
            Long commentId,
            UpdateCommentUserRequest dto
    ) {
        if (dto.getStatus() != null
                && dto.getStatus() != CommentStatus.DELETED) {

            throw new ConditionsNotMetException(
                    "User can only set status to DELETED"
            );
        }

        Comment comment = getExistingComment(commentId);

        if (!Objects.equals(
                comment.getAuthorId(),
                userId
        )) {
            throw new ConditionsNotMetException(
                    "Only author or admin can update comments"
            );
        }

        if (dto.getText() != null
                && comment.getStatus()
                != CommentStatus.PENDING) {

            throw new ConditionsNotMetException(
                    "Text can only be changed when "
                            + "comment is in PENDING status"
            );
        }

        CommentMapper.updateCommentFromUserRequest(
                dto,
                comment
        );

        return CommentMapper.toFullDto(
                commentRepository.save(comment)
        );
    }

    @Override
    @Transactional
    public void deleteCommentByUser(
            Long userId,
            Long commentId
    ) {
        Comment comment = getExistingComment(commentId);

        if (!Objects.equals(
                comment.getAuthorId(),
                userId
        )) {
            throw new ConditionsNotMetException(
                    "Only author or admin can delete comments"
            );
        }

        comment.setStatus(CommentStatus.DELETED);
        comment.setUpdated(LocalDateTime.now());

        commentRepository.save(comment);
    }

    @Override
    public List<CommentFullDto>
    getCommentsForModeration(
            int from,
            int size
    ) {
        PageRequest page = PageRequest.of(
                from / size,
                size
        );

        return commentRepository
                .findByStatusOrderByCreatedAsc(
                        CommentStatus.PENDING,
                        page
                )
                .stream()
                .map(CommentMapper::toFullDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentFullDto moderateComment(
            Long moderatorId,
            Long commentId,
            UpdateCommentAdminRequest dto
    ) {
        Comment comment = getExistingComment(commentId);

        UserDetailsDto moderator =
                userServiceClient.getUser(moderatorId);

        CommentMapper.updateCommentFromAdminRequest(
                dto,
                comment,
                moderator
        );

        return CommentMapper.toFullDto(
                commentRepository.save(comment)
        );
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(
            Long moderatorId,
            Long commentId
    ) {
        Comment comment = getExistingComment(commentId);

        UserDetailsDto moderator =
                userServiceClient.getUser(moderatorId);

        CommentMapper.adminDeleteComment(
                comment,
                moderator
        );

        commentRepository.save(comment);
    }

    @Override
    public ApprovedCommentsResponse
    getApprovedCommentCounts(
            CommentEventIdsRequest request
    ) {
        if (request == null
                || request.getEventIds() == null
                || request.getEventIds().isEmpty()) {

            return ApprovedCommentsResponse.builder()
                    .approvedComments(Collections.emptyMap())
                    .build();
        }

        List<Long> eventIds = request.getEventIds()
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (eventIds.isEmpty()) {
            return ApprovedCommentsResponse.builder()
                    .approvedComments(Collections.emptyMap())
                    .build();
        }

        List<Object[]> rows =
                commentRepository
                        .countByEventIdInAndStatus(
                                eventIds,
                                CommentStatus.APPROVED
                        );

        Map<Long, Long> counts = new LinkedHashMap<>();

        for (Object[] row : rows) {
            Long eventId =
                    ((Number) row[0]).longValue();

            Long count =
                    ((Number) row[1]).longValue();

            counts.put(eventId, count);
        }

        return ApprovedCommentsResponse.builder()
                .approvedComments(counts)
                .build();
    }

    private Comment getExistingComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Comment with id="
                                        + commentId
                                        + " not found"
                        )
                );
    }
}