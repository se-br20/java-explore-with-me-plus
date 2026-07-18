package ru.practicum.ewm.comments.dto;

import ru.practicum.ewm.comments.model.Comment;
import ru.practicum.ewm.comments.model.CommentStatus;
import ru.practicum.interaction.event.EventDetailsDto;
import ru.practicum.interaction.user.UserDetailsDto;
import ru.practicum.interaction.user.UserShortDto;

import java.time.LocalDateTime;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static Comment toComment(
            NewCommentDto dto,
            UserDetailsDto author,
            EventDetailsDto event
    ) {
        return Comment.builder()
                .text(dto.getText())
                .authorId(author.getId())
                .authorName(author.getName())
                .eventId(event.getId())
                .created(LocalDateTime.now())
                .status(CommentStatus.PENDING)
                .build();
    }

    public static void updateCommentFromUserRequest(
            UpdateCommentUserRequest request,
            Comment comment
    ) {
        boolean updated = false;

        if (request.getText() != null) {
            comment.setText(request.getText());
            updated = true;
        }

        if (request.getStatus() != null) {
            comment.setStatus(CommentStatus.DELETED);
            updated = true;
        }

        if (updated) {
            comment.setUpdated(LocalDateTime.now());
        }
    }

    public static void updateCommentFromAdminRequest(
            UpdateCommentAdminRequest request,
            Comment comment,
            UserDetailsDto moderator
    ) {
        boolean textUpdated = false;

        if (request.getText() != null) {
            comment.setText(request.getText());
            textUpdated = true;
        }

        if (request.getStatus() != null) {
            comment.setStatus(request.getStatus());
            comment.setModeratorId(moderator.getId());
            comment.setModeratorName(moderator.getName());
            comment.setModeratedAt(LocalDateTime.now());
        }

        if (textUpdated) {
            comment.setUpdated(LocalDateTime.now());
        }
    }

    public static CommentFullDto toFullDto(
            Comment comment
    ) {
        CommentFullDto.CommentFullDtoBuilder builder =
                CommentFullDto.builder()
                        .id(comment.getId())
                        .text(comment.getText())
                        .author(
                                toUserShortDto(
                                        comment.getAuthorId(),
                                        comment.getAuthorName()
                                )
                        )
                        .eventId(comment.getEventId())
                        .created(comment.getCreated())
                        .updated(comment.getUpdated())
                        .status(comment.getStatus().name());

        if (comment.getModeratorId() != null) {
            builder.moderator(
                            toUserShortDto(
                                    comment.getModeratorId(),
                                    comment.getModeratorName()
                            )
                    )
                    .moderatedAt(comment.getModeratedAt());
        }

        return builder.build();
    }

    public static CommentShortDto toShortDto(
            Comment comment
    ) {
        return CommentShortDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(
                        toUserShortDto(
                                comment.getAuthorId(),
                                comment.getAuthorName()
                        )
                )
                .eventId(comment.getEventId())
                .created(comment.getCreated())
                .build();
    }

    public static void adminDeleteComment(
            Comment comment,
            UserDetailsDto moderator
    ) {
        comment.setStatus(CommentStatus.DELETED);
        comment.setModeratorId(moderator.getId());
        comment.setModeratorName(moderator.getName());
        comment.setModeratedAt(LocalDateTime.now());
        comment.setUpdated(LocalDateTime.now());
    }

    private static UserShortDto toUserShortDto(
            Long userId,
            String userName
    ) {
        return UserShortDto.builder()
                .id(userId)
                .name(userName)
                .build();
    }
}