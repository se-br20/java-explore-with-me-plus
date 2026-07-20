package ru.practicum.ewm.user.service;

import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.interaction.user.UserDetailsDto;

import java.util.List;

public interface UserService {

    UserDto createUser(NewUserRequest newUserRequest);

    List<UserDto> findUsers(
            List<Long> ids,
            int from,
            int size
    );

    void deleteUser(Long userId);

    UserDetailsDto getUserDetails(Long userId);
}