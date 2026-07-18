package ru.practicum.ewm.interaction.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.interaction.comment.ApprovedCommentsResponse;

import java.util.Collections;

@Slf4j
@Component
public class CommentServiceClientFallbackFactory
        implements FallbackFactory<CommentServiceClient> {

    @Override
    public CommentServiceClient create(Throwable cause) {
        log.warn(
                "comment-service is unavailable. "
                        + "Comment counts will be replaced with 0. "
                        + "Cause: {}",
                cause.getMessage()
        );

        return request ->
                ApprovedCommentsResponse.builder()
                        .approvedComments(
                                Collections.emptyMap()
                        )
                        .build();
    }
}