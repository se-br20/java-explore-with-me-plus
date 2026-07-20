package ru.practicum.ewm.interaction.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.interaction.request.ConfirmedRequestsResponse;
import ru.practicum.interaction.request.EventIdsRequest;

import java.util.Collections;

@Slf4j
@Component
public class RequestServiceClientFallbackFactory
        implements FallbackFactory<RequestServiceClient> {

    @Override
    public RequestServiceClient create(Throwable cause) {
        log.warn(
                "request-service is unavailable. "
                        + "Confirmed request counts will be "
                        + "replaced with 0 and LIKE permission "
                        + "will be denied. Cause: {}",
                cause.getMessage()
        );

        return new RequestServiceClient() {

            @Override
            public ConfirmedRequestsResponse
            getConfirmedRequestCounts(
                    EventIdsRequest request
            ) {
                return ConfirmedRequestsResponse.builder()
                        .confirmedRequests(
                                Collections.emptyMap()
                        )
                        .build();
            }

            @Override
            public boolean hasConfirmedRequest(
                    Long userId,
                    Long eventId
            ) {
                return false;
            }
        };
    }
}