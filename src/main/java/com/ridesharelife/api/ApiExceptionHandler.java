package com.ridesharelife.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ErrorResponse(String error) {}
    @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse notFound(NoSuchElementException ex) { return new ErrorResponse(ex.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse badRequest(IllegalArgumentException ex) { return new ErrorResponse(ex.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField()+": "+e.getDefaultMessage()).findFirst().orElse("Invalid request.");
        return new ErrorResponse(message);
    }
    @ExceptionHandler(HandlerMethodValidationException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse parameterValidation(HandlerMethodValidationException ex) { return new ErrorResponse("Required parameters are missing or invalid."); }
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse malformedRequest(Exception ex) { return new ErrorResponse("Request body or required parameters are missing or invalid."); }
    @ExceptionHandler(DataIntegrityViolationException.class) @ResponseStatus(HttpStatus.CONFLICT)
    ErrorResponse conflict(DataIntegrityViolationException ex) { return new ErrorResponse("This record conflicts with existing data."); }
}
