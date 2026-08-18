package com.accesscontrol.api.exception;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.accesscontrol.api.dto.ErrorResponse;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;


@RestControllerAdvice
public class GlobalExceptionHandler {
	@Hidden
    // Handle standard business logic exceptions (e.g., RuntimeException)
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
	    ex.printStackTrace(); // add this — you've been debugging blind without it
	    String requestId = MDC.get("requestId");
	    ErrorResponse error = ErrorResponse.builder()
	            .timestamp(OffsetDateTime.now())
	            .status(HttpStatus.BAD_REQUEST.value())
	            .error("Bad Request")
	            .message(ex.getMessage())
	            .path(request.getRequestURI())
	            .requestId(requestId)
	            .build();
	    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST); // fix: match the body's claimed status
	}	
	@Hidden
    // Handle validation failures (@Valid on DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = MDC.get("requestId"); // Fetch Request ID from MDC

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(errorMessage)
                .path(request.getRequestURI())
                .requestId(requestId) // Pass it here
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
	
	@Hidden
    // Handle all other unexpected server errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
		ex.printStackTrace();
        String requestId = MDC.get("requestId"); // Fetch Request ID from MDC

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .requestId(requestId) // Pass it here
                .build();

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}