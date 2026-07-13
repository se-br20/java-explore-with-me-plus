package ru.practicum.stats.server.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.stats.server.exceptions.responseMessage.ApiError;
import ru.practicum.stats.server.exceptions.responseMessage.ValidationError;
import ru.practicum.stats.server.exceptions.responseMessage.ValidationErrorResponse;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        List<ValidationError> validationErrors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toValidationError)
                .toList();

        return new ValidationErrorResponse(
                "VALIDATION_FAILED",
                validationErrors
        );
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidationException(
            ValidationException exception
    ) {
        ValidationError error = new ValidationError(
                exception.getFieldName(),
                exception.getMessage(),
                exception.getRejectedValue()
        );

        return new ValidationErrorResponse(
                "VALIDATION_FAILED",
                List.of(error)
        );
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(Exception exception) {
        log.warn("Некорректный запрос к stats-server: {}", exception.getMessage());

        return new ApiError(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception exception) {
        log.error("Внутренняя ошибка stats-server", exception);

        return new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                null
        );
    }

    private ValidationError toValidationError(FieldError fieldError) {
        return new ValidationError(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
        );
    }
}