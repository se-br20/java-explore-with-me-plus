package ru.practicum.ewm.comments.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.comments.service.CommentService;
import ru.practicum.interaction.comment.ApprovedCommentsResponse;
import ru.practicum.interaction.comment.CommentEventIdsRequest;

@RestController
@RequestMapping("/internal/comments")
@RequiredArgsConstructor
public class InternalCommentController {

    private final CommentService commentService;

    @PostMapping("/approved-counts")
    public ApprovedCommentsResponse
    getApprovedCommentCounts(
            @Valid @RequestBody
            CommentEventIdsRequest request
    ) {
        return commentService
                .getApprovedCommentCounts(request);
    }
}