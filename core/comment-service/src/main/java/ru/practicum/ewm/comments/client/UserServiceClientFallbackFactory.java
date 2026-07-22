package ru.practicum.ewm.comments.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.ewm.exceptions.exceptions.ServiceUnavailableException;
import ru.practicum.interaction.user.UserDetailsDto;

@Slf4j
@Component
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        log.warn(
                "Failed to call user-service from comment-service: {}",
                cause.getMessage()
        );

        return new UserServiceClient() {

            @Override
            public UserDetailsDto getUser(Long userId) {
                if (containsNotFound(cause)) {
                    throw new NotFoundException(
                            "User with id="
                                    + userId
                                    + " was not found"
                    );
                }

                throw new ServiceUnavailableException(
                        "Unable to get user data from user-service",
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