package com.forex.api.exception;


import org.springframework.http.HttpStatus;

public class ForexAppException extends RuntimeException {
    private final HttpStatus status;

    public ForexAppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ForexAppException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR); // default fallback
    }

    public HttpStatus getStatus() {
        return status;
    }
}
