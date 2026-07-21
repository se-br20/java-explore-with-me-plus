package ru.practicum.stats.server.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        List<FieldError> fieldErrors =
                exception.getBindingResult().getFieldErrors();

        List<ValidationError> validationErrors = fieldErrors.stream()
                .map(fieldError -> new ValidationError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        fieldError.getRejectedValue()
                ))
                .collect(Collectors.toList());

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
        List<ValidationError> validationErrors = List.of(
                new ValidationError(
                        exception.getFieldName(),
                        exception.getMessage(),
                        exception.getRejectedValue()
                )
        );

        return new ValidationErrorResponse(
                "VALIDATION_FAILED",
                validationErrors
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleMissingRequestParameter(
            MissingServletRequestParameterException exception
    ) {
        log.warn(
                "Required request parameter is missing: {}",
                exception.getParameterName()
        );

        ValidationError validationError = new ValidationError(
                exception.getParameterName(),
                exception.getMessage(),
                null
        );

        return new ValidationErrorResponse(
                "VALIDATION_FAILED",
                List.of(validationError)
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        log.warn(
                "Request parameter has an invalid value: parameter={}, value={}",
                exception.getName(),
                exception.getValue()
        );

        ValidationError validationError = new ValidationError(
                exception.getName(),
                "Invalid parameter format",
                exception.getValue()
        );

        return new ValidationErrorResponse(
                "VALIDATION_FAILED",
                List.of(validationError)
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception exception) {
        log.error("Unexpected stats-server error", exception);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);

        return new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                stringWriter.toString()
        );
    }
}