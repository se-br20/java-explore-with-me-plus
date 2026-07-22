package ru.practicum.ewm.comments.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.comments.client.EventServiceClient;
import ru.practicum.ewm.comments.client.UserServiceClient;
import ru.practicum.ewm.comments.dto.UpdateCommentAdminRequest;
import ru.practicum.ewm.comments.model.CommentStatus;
import ru.practicum.ewm.comments.repository.CommentRepository;
import ru.practicum.ewm.exceptions.exceptions.ConditionsNotMetException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommentServiceImplTest {

    private CommentRepository commentRepository;
    private UserServiceClient userServiceClient;
    private EventServiceClient eventServiceClient;
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        commentRepository =
                mock(CommentRepository.class);

        userServiceClient =
                mock(UserServiceClient.class);

        eventServiceClient =
                mock(EventServiceClient.class);

        commentService =
                new CommentServiceImpl(
                        commentRepository,
                        userServiceClient,
                        eventServiceClient
                );
    }

    @Test
    void shouldUseFromAsExactOffset() {
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(
                        Pageable.class
                );

        when(
                commentRepository
                        .findByEventIdAndStatusOrderByCreatedDesc(
                                eq(10L),
                                eq(CommentStatus.APPROVED),
                                pageableCaptor.capture()
                        )
        ).thenReturn(List.of());

        commentService.getEventComments(
                10L,
                5,
                10
        );

        Pageable pageable =
                pageableCaptor.getValue();

        assertEquals(
                5L,
                pageable.getOffset()
        );

        assertEquals(
                10,
                pageable.getPageSize()
        );

        verify(commentRepository)
                .findByEventIdAndStatusOrderByCreatedDesc(
                        10L,
                        CommentStatus.APPROVED,
                        pageable
                );
    }

    @Test
    void shouldRejectPendingStatusFromAdmin() {
        UpdateCommentAdminRequest request =
                UpdateCommentAdminRequest.builder()
                        .status(
                                CommentStatus.PENDING
                        )
                        .build();

        assertThrows(
                ConditionsNotMetException.class,
                () -> commentService
                        .moderateComment(
                                1L,
                                10L,
                                request
                        )
        );

        verifyNoInteractions(
                commentRepository,
                userServiceClient,
                eventServiceClient
        );
    }
}