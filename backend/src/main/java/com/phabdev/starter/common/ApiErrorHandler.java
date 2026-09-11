package com.phabdev.starter.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;

@RestControllerAdvice
public class ApiErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    @ExceptionHandler(ApiException.class)
    ProblemDetail api(ApiException ex) {
        return ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex) {
        var detail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Check the submitted fields.");
        var errors = new LinkedHashMap<String, String>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    ProblemDetail invalid(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(Exception ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The operation conflicts with existing data.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail denied(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse error) {
            return ProblemDetail.forStatusAndDetail(
                    error.getStatusCode(),
                    error.getStatusCode().value() == 404
                            ? "Resource not found."
                            : "Invalid request.");
        }
        // Never log request bodies, credentials, JWTs or reset links.
        log.error("Unhandled API failure: {}", ex.getClass().getSimpleName());
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");
    }
}
