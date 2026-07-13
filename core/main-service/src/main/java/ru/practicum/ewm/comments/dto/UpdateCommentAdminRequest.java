package ru.practicum.ewm.comments.dto;

import jakarta.validation.constraints.Pattern;
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
public class UpdateCommentAdminRequest {

    @Size(min = 1, max = 2000)
    @Pattern(
            regexp = "(?s).*\\S.*",
            message = "Comment text must contain a non-whitespace character"
    )
    private String text;

    private CommentStatus status;
}