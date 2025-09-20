package org.linkgenetic.resolver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LinkIdNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLinkIdNotFound(
            LinkIdNotFoundException ex, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "LinkID Not Found");
        response.put("message", ex.getMessage());
        response.put("linkId", ex.getLinkId());
        response.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidLinkIdFormatException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidLinkIdFormat(
            InvalidLinkIdFormatException ex, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Invalid LinkID Format");
        response.put("message", ex.getMessage());
        response.put("linkId", ex.getLinkId());
        response.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LinkIdWithdrawnException.class)
    public ResponseEntity<Map<String, Object>> handleLinkIdWithdrawn(
            LinkIdWithdrawnException ex, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now());
        response.put("status", HttpStatus.GONE.value());
        response.put("error", "LinkID Withdrawn");
        response.put("message", ex.getMessage());
        response.put("linkId", ex.getLinkId());
        response.put("tombstone", ex.getTombstone());
        response.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(response, HttpStatus.GONE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred");
        response.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}