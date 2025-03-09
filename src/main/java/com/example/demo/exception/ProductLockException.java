package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)   // HTTP 429 : 请求过多
public class ProductLockException extends RuntimeException{
    public ProductLockException(String message) {
        super(message);
    }
}
