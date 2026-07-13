package ru.practicum.ewm.comments.dto;

import ru.practicum.ewm.comments.model.Comment;
import ru.practicum.ewm.comments.model.CommentStatus;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.user.dto.UserMapper;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static Comment toComment(
            NewCommentDto dto,
            User author,
            Event event
    ) {
        return Comment.builder()
                .text(dto.getText())
                .author(author)
                .event(event)
                .created(LocalDateTime.now())
                .status(CommentStatus.PENDING)
                .build();
    }

    public static void updateCommentFromUserRequest(
            UpdateCommentUserRequest request,
            Comment comment
    ) {
        boolean changed = false;

        if (request.getText() != null) {
            comment.setText(request.getText());
            changed = true;
        }

        if (request.getStatus() != null) {
            comment.setStatus(request.getStatus());
            changed = true;
        }

        if (changed) {
            comment.setUpdated(LocalDateTime.now());
        }
    }

    public static void updateCommentFromAdminRequest(
            UpdateCommentAdminRequest request,
            Comment comment,
            User moderator
    ) {
        if (request.getText() != null) {
            comment.setText(request.getText());
            comment.setUpdated(LocalDateTime.now());
        }

        if (request.getStatus() != null) {
            comment.setStatus(request.getStatus());
        }
        comment.setModerator(moderator);
        comment.setModeratedAt(LocalDateTime.now());
    }

    public static CommentFullDto toFullDto(Comment comment) {
        return CommentFullDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(UserMapper.toUserShortDto(comment.getAuthor()))
                .eventId(comment.getEvent().getId())
                .created(comment.getCreated())
                .updated(comment.getUpdated())
                .status(comment.getStatus())
                .moderator(
                        comment.getModerator() == null
                                ? null
                                : UserMapper.toUserShortDto(
                                comment.getModerator()
                        )
                )
                .moderatedAt(comment.getModeratedAt())
                .build();
    }

    public static CommentShortDto toShortDto(Comment comment) {
        return CommentShortDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(UserMapper.toUserShortDto(comment.getAuthor()))
                .eventId(comment.getEvent().getId())
                .created(comment.getCreated())
                .build();
    }

    public static void adminDeleteComment(
            Comment comment,
            User moderator
    ) {
        LocalDateTime now = LocalDateTime.now();

        comment.setStatus(CommentStatus.DELETED);
        comment.setModerator(moderator);
        comment.setModeratedAt(now);
        comment.setUpdated(now);
    }
}