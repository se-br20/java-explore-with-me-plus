package ru.practicum.ewm.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.dto.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.interaction.user.UserDetailsDto;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Transactional
    @Override
    public UserDto createUser(NewUserRequest newUserRequest) {
        // Email не проверяется на уникальность, т.к. отлавливается DataIntegrityViolationException при нарушении UNIQUE в БД
        User user = userRepository.save(UserMapper.toUser(newUserRequest));
        log.info("Created user with id: {}", user.getId());

        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> findUsers(List<Long> ids, int from, int size) {
        List<UserDto> users = userRepository.findUsers(ids, from, size);
        log.debug("Found {} users", users.size());

        return users;
    }

    @Transactional
    @Override
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        userRepository.deleteById(userId);
        log.info("Deleted user with id: {}", userId);
    }

    @Override
    public UserDetailsDto getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "User with id="
                                        + userId
                                        + " was not found"
                        )
                );

        return UserMapper.toUserDetailsDto(user);
    }


}
