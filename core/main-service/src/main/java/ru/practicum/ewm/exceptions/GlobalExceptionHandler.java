package ru.practicum.ewm.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.ewm.exceptions.exceptions.ConditionsNotMetException;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.ewm.exceptions.exceptions.ValidationException;
import ru.practicum.ewm.exceptions.responseMessage.ApiError;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        String message = formatFieldErrors(
                exception.getBindingResult()
                        .getFieldErrors()
        );

        log.warn(
                "Request body validation error: {}",
                message
        );

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                message
        );
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBindException(
            BindException exception
    ) {
        String message = formatFieldErrors(
                exception.getBindingResult()
                        .getFieldErrors()
        );

        log.warn(
                "Request parameters validation error: {}",
                message
        );

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                message
        );
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(
            ValidationException exception
    ) {
        String message = String.format(
                "Field: %s. Error: %s. Value: %s",
                exception.getFieldName(),
                exception.getMessage(),
                exception.getRejectedValue()
        );

        log.warn(
                "Validation error: {}",
                message
        );

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                message
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        String message =
                exception.getMostSpecificCause()
                        .getMessage();

        log.warn(
                "Database integrity violation: {}",
                message
        );

        return buildError(
                HttpStatus.CONFLICT,
                "Integrity constraint has been violated.",
                message
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        String message =
                exception.getConstraintViolations()
                        .stream()
                        .map(violation ->
                                String.format(
                                        "Field: %s. Error: %s. Value: %s",
                                        violation.getPropertyPath(),
                                        violation.getMessage(),
                                        violation.getInvalidValue()
                                )
                        )
                        .collect(
                                Collectors.joining("; ")
                        );

        log.warn(
                "Constraint violation: {}",
                message
        );

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                message
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        log.warn(
                "Argument type mismatch: {}",
                exception.getMessage()
        );

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                exception.getMessage()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception
    ) {
        log.warn(
                "Cannot read request body: {}",
                exception.getMessage()
        );

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                exception.getMessage()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception
    ) {
        log.warn(
                "Missing request parameter: {}",
                exception.getMessage()
        );

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                exception.getMessage()
        );
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(
            NotFoundException exception
    ) {
        log.info(
                "Resource not found: {}",
                exception.getMessage()
        );

        return buildError(
                HttpStatus.NOT_FOUND,
                "The required object was not found.",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ConditionsNotMetException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConditionsNotMetException(
            ConditionsNotMetException exception
    ) {
        log.warn(
                "Conditions not met: {}",
                exception.getMessage()
        );

        return buildError(
                HttpStatus.CONFLICT,
                "For the requested operation "
                        + "the conditions are not met.",
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(
            Exception exception
    ) {
        log.error(
                "Internal server error",
                exception
        );

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error.",
                exception.getMessage()
        );
    }

    private String formatFieldErrors(
            List<FieldError> fieldErrors
    ) {
        return fieldErrors.stream()
                .map(error ->
                        String.format(
                                "Field: %s. Error: %s. Value: %s",
                                error.getField(),
                                error.getDefaultMessage(),
                                error.getRejectedValue()
                        )
                )
                .collect(
                        Collectors.joining("; ")
                );
    }

    private ApiError buildError(
            HttpStatus status,
            String reason,
            String message
    ) {
        return ApiError.builder()
                .errors(Collections.emptyList())
                .message(message)
                .reason(reason)
                .status(status.name())
                .timestamp(
                        LocalDateTime.now()
                                .format(FORMATTER)
                )
                .build();
    }
}
