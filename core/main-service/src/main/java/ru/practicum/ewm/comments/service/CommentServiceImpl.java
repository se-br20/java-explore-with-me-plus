package ru.practicum.ewm.comments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comments.dto.CommentFullDto;
import ru.practicum.ewm.comments.dto.CommentMapper;
import ru.practicum.ewm.comments.dto.CommentShortDto;
import ru.practicum.ewm.comments.dto.NewCommentDto;
import ru.practicum.ewm.comments.dto.UpdateCommentAdminRequest;
import ru.practicum.ewm.comments.dto.UpdateCommentUserRequest;
import ru.practicum.ewm.comments.model.Comment;
import ru.practicum.ewm.comments.model.CommentStatus;
import ru.practicum.ewm.comments.repository.CommentRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.exceptions.ConditionsNotMetException;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.ewm.exceptions.exceptions.ValidationException;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private static final Set<CommentStatus> ADMIN_STATUSES =
            EnumSet.of(
                    CommentStatus.APPROVED,
                    CommentStatus.REJECTED,
                    CommentStatus.DELETED
            );

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public List<CommentShortDto> getEventComments(
            Long eventId,
            int from,
            int size
    ) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException(
                    "Event with id=" + eventId + " was not found"
            );
        }

        PageRequest pageRequest = PageRequest.of(from, size);

        return commentRepository
                .findByEventIdAndStatusOrderByCreatedDesc(
                        eventId,
                        CommentStatus.APPROVED,
                        pageRequest
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
                .orElseThrow(() -> new NotFoundException(
                        "Comment with id=" + commentId + " was not found"
                ));

        return CommentMapper.toShortDto(comment);
    }

    @Override
    @Transactional
    public CommentFullDto createComment(
            Long userId,
            Long eventId,
            NewCommentDto dto
    ) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "User with id=" + userId + " was not found"
                ));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        "Event with id=" + eventId + " was not found"
                ));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConditionsNotMetException(
                    "Cannot comment on an unpublished event"
            );
        }

        Comment comment = CommentMapper.toComment(dto, author, event);

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
        validateUserUpdate(dto);

        Comment comment = commentRepository
                .findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Comment with id=" + commentId
                                + " was not found for user with id=" + userId
                ));

        if (dto.getText() != null
                && comment.getStatus() != CommentStatus.PENDING) {
            throw new ConditionsNotMetException(
                    "Text can only be changed when comment is in PENDING status"
            );
        }

        CommentMapper.updateCommentFromUserRequest(dto, comment);

        return CommentMapper.toFullDto(
                commentRepository.save(comment)
        );
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        Comment comment = commentRepository
                .findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Comment with id=" + commentId
                                + " was not found for user with id=" + userId
                ));

        CommentMapper.updateCommentFromUserRequest(
                UpdateCommentUserRequest.builder()
                        .status(CommentStatus.DELETED)
                        .build(),
                comment
        );

        commentRepository.save(comment);
    }

    @Override
    public List<CommentFullDto> getCommentsForModeration(
            int from,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(from, size);

        return commentRepository
                .findByStatusOrderByCreatedAsc(
                        CommentStatus.PENDING,
                        pageRequest
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
        validateAdminUpdate(dto);

        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new NotFoundException(
                        "Moderator with id=" + moderatorId + " was not found"
                ));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(
                        "Comment with id=" + commentId + " was not found"
                ));

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
        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new NotFoundException(
                        "Moderator with id=" + moderatorId + " was not found"
                ));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(
                        "Comment with id=" + commentId + " was not found"
                ));

        CommentMapper.adminDeleteComment(comment, moderator);
        commentRepository.save(comment);
    }

    private void validateUserUpdate(UpdateCommentUserRequest dto) {
        if (dto.getText() == null && dto.getStatus() == null) {
            throw new ValidationException(
                    "body",
                    dto,
                    "At least one field must be provided"
            );
        }

        if (dto.getStatus() != null
                && dto.getStatus() != CommentStatus.DELETED) {
            throw new ConditionsNotMetException(
                    "User can only set comment status to DELETED"
            );
        }
    }

    private void validateAdminUpdate(UpdateCommentAdminRequest dto) {
        if (dto.getText() == null && dto.getStatus() == null) {
            throw new ValidationException(
                    "body",
                    dto,
                    "At least one field must be provided"
            );
        }

        if (dto.getStatus() != null
                && !ADMIN_STATUSES.contains(dto.getStatus())) {
            throw new ConditionsNotMetException(
                    "Admin can set status only to APPROVED, REJECTED or DELETED"
            );
        }
    }
}