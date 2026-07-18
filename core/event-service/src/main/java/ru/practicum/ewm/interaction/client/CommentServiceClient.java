package ru.practicum.ewm.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.interaction.comment.ApprovedCommentsResponse;
import ru.practicum.interaction.comment.CommentEventIdsRequest;

@FeignClient(
        name = "comment-service",
        path = "/internal/comments",
        fallbackFactory =
                CommentServiceClientFallbackFactory.class
)
public interface CommentServiceClient {

    @PostMapping("/approved-counts")
    ApprovedCommentsResponse getApprovedCommentCounts(
            @RequestBody CommentEventIdsRequest request
    );
}