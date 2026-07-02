package org.example.ai408.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        return ResponseEntity.ok(new ApiResponse<>(exception.getErrorCode().code(), exception.getMessage(), null));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception exception) {
        String message = "参数校验失败";
        if (exception instanceof MethodArgumentNotValidException methodException && methodException.getBindingResult().hasErrors()) {
            message = Objects.requireNonNull(methodException.getBindingResult().getFieldError()).getDefaultMessage();
        } else if (exception instanceof BindException bindException && bindException.hasErrors()) {
            message = Objects.requireNonNull(bindException.getFieldError()).getDefaultMessage();
        } else if (exception instanceof ConstraintViolationException constraintViolationException
                && !constraintViolationException.getConstraintViolations().isEmpty()) {
            message = constraintViolationException.getConstraintViolations().iterator().next().getMessage();
        }
        return ResponseEntity.ok(new ApiResponse<>(ErrorCode.VALIDATION_FAILED.code(), message, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message(), null));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(ErrorCode.UNAUTHORIZED.code(), ErrorCode.UNAUTHORIZED.message(), null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException exception) {
        return ResponseEntity.ok(new ApiResponse<>(ErrorCode.CONFLICT.code(), ErrorCode.CONFLICT.message(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.ok(new ApiResponse<>(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.message(), null));
    }
}
