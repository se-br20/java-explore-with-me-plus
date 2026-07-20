package ru.practicum.ewm.request.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.ewm.exceptions.exceptions.ServiceUnavailableException;
import ru.practicum.interaction.event.EventDetailsDto;

@Slf4j
@Component
public class EventServiceClientFallbackFactory
        implements FallbackFactory<EventServiceClient> {

    @Override
    public EventServiceClient create(Throwable cause) {
        log.warn(
                "Failed to call event-service from request-service: {}",
                cause.getMessage()
        );

        return new EventServiceClient() {

            @Override
            public EventDetailsDto getEvent(Long eventId) {
                if (containsNotFound(cause)) {
                    throw new NotFoundException(
                            "Event with id="
                                    + eventId
                                    + " was not found"
                    );
                }

                throw new ServiceUnavailableException(
                        "Unable to get event data from event-service",
                        cause
                );
            }
        };
    }

    private boolean containsNotFound(Throwable throwable) {
        Throwable current = throwable;

        for (int depth = 0;
             current != null && depth < 10;
             depth++) {

            if (current instanceof FeignException.NotFound) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}