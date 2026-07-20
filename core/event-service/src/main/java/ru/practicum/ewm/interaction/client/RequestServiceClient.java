package ru.practicum.ewm.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.interaction.request.ConfirmedRequestsResponse;
import ru.practicum.interaction.request.EventIdsRequest;

@FeignClient(
        name = "request-service",
        path = "/internal/requests",
        fallbackFactory = RequestServiceClientFallbackFactory.class
)
public interface RequestServiceClient {

    @PostMapping("/confirmed-counts")
    ConfirmedRequestsResponse getConfirmedRequestCounts(
            @RequestBody EventIdsRequest request
    );
}