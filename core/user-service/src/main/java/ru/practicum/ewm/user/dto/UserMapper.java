package ru.practicum.ewm.user.dto;

import ru.practicum.ewm.user.model.User;
import ru.practicum.interaction.user.UserDetailsDto;

public class UserMapper {

    public static User toUser(NewUserRequest request) {
        return User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .build();
    }

    public static UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    public static UserShortDto toUserShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }

    public static UserDetailsDto toUserDetailsDto(User user) {
        return UserDetailsDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}