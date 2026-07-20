package ru.practicum.ewm.comments.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.comments.model.CommentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentUserRequest {

    @Size(
            min = 1,
            max = 2000
    )
    private String text;

    private CommentStatus status;
}