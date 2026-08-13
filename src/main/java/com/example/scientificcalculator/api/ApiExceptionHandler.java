package com.example.scientificcalculator.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.scientificcalculator.calculation.HistoryNotFoundException;
import com.example.scientificcalculator.expression.ExpressionException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ExpressionException.class)
    ResponseEntity<ApiError> expression(ExpressionException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.code()) {
            case "DIVISION_BY_ZERO", "DOMAIN_ERROR", "NON_FINITE_RESULT" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.BAD_REQUEST;
        };
        return error(status, exception.code(), exception.getMessage(), request);
    }

    @ExceptionHandler(HistoryNotFoundException.class)
    ResponseEntity<ApiError> historyNotFound(HistoryNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "HISTORY_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class})
    ResponseEntity<ApiError> invalidArgument(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", "Invalid request", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> unsupportedMediaType(HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Content-Type is not supported",
                request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> internal(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(code, message, request.getRequestURI(), Instant.now()));
    }

    record ApiError(String code, String message, String path, Instant timestamp) {
    }
}
