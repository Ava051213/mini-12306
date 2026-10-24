package com.mia.mini12306.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(
            OrderNotFoundException exception
    ) {
        // Keep business errors in the existing response format.
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(OrderStateConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleStateConflict(
            OrderStateConflictException exception
    ) {
        return new ErrorResponse(exception.getMessage());
    }

    public record ErrorResponse(String message) {

    }

}
