package com.example.DebitCopybook.exception;

import com.example.DebitCopybook.controller.RecurringDebtController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = RecurringDebtController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RecurringDebtExceptionHandler {

    @ExceptionHandler(RecurringDebtException.class)
    public ResponseEntity<Object> handle(RecurringDebtException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatus().value());
        body.put("error", "Yanlış Sorğu");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(body, ex.getStatus());
    }
}