package com.example.DebitCopybook.exception;

import org.springframework.http.HttpStatus;

public class RecurringDebtException extends RuntimeException {
    private final HttpStatus status;

    public RecurringDebtException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public RecurringDebtException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}