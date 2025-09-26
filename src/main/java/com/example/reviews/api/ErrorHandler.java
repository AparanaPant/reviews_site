package com.example.reviews.api;

import com.example.reviews.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global error handler for REST APIs.
 *
 * This class makes sure that whenever something goes wrong,
 * we send back a clean JSON response with an error code + message,
 * instead of a raw stack trace.
 *
 * Example response:
 * {
 *   "error": "NOT_FOUND",
 *   "message": "Review 42 not found"
 * }
 */
@RestControllerAdvice
public class ErrorHandler {

    /**
     * Handle "review not found" errors from our service layer.
     * Maps to a 404 response.
     */
    @ExceptionHandler(ReviewService.NotFoundException.class)
    public ResponseEntity<Map<String,Object>> notFound(ReviewService.NotFoundException ex) {
        return error("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Handle validation errors (e.g. invalid request payloads).
     * Maps to a 400 response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> badRequest(MethodArgumentNotValidException ex) {
        return error("BAD_REQUEST", ex.getBindingResult().toString(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle bad input from client code.
     * Maps to a 400 response.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> badRequest(IllegalArgumentException ex) {
        return error("BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Fallback: catch anything else we didn’t handle specifically.
     * Maps to a 500 response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> generic(Exception ex) {
        return error("INTERNAL_ERROR", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Small helper to build a consistent JSON error response.
     */
    private ResponseEntity<Map<String,Object>> error(String code, String msg, HttpStatus status) {
        Map<String,Object> body = new HashMap<>();
        body.put("error", code);
        body.put("message", msg);
        return new ResponseEntity<>(body, status);
    }
}
