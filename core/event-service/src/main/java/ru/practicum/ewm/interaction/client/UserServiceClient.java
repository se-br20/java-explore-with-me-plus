package ru.practicum.ewm.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.interaction.user.UserDetailsDto;

@FeignClient(
        name = "user-service",
        path = "/internal/users",
        fallbackFactory = UserServiceClientFallbackFactory.class
)
public interface UserServiceClient {

    @GetMapping("/{userId}")
    UserDetailsDto getUser(
            @PathVariable("userId") Long userId
    );
}