package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class CacheConversionException extends RuntimeException {
    public CacheConversionException(String message, Exception e) {
        super(message, e);
    }
}
