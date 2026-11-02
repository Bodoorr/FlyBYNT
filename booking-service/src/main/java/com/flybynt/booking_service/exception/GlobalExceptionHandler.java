package com.flybynt.booking_service.exception;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.client.RestClientException;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String,String>> api(ApiException ex){
        return ResponseEntity.status(ex.status).body(Map.of("error",ex.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String,String>> duplicate(DuplicateKeyException ex){
        return ResponseEntity.status(409).body(Map.of("error","Duplicate active booking or idempotency key"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException ex){
        return ResponseEntity.badRequest().body(Map.of("error","Invalid request"));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String,String>> downstream(RestClientException ex){
        return ResponseEntity.status(503).body(Map.of("error","Downstream service unavailable; booking outcome may need reconciliation"));
    }
}
