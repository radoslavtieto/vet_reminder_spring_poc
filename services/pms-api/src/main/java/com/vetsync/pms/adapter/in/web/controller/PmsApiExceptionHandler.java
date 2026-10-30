package com.vetsync.pms.adapter.in.web.controller;

import com.vetsync.pms.application.PmsInvalidRequestException;
import com.vetsync.pms.application.PmsResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PmsApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(PmsApiExceptionHandler.class);

    @ExceptionHandler(PmsResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(PmsResourceNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(PmsInvalidRequestException.class)
    public ResponseEntity<ProblemDetail> invalidRequest(PmsInvalidRequestException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validationFailure(MethodArgumentNotValidException exception) {
        logger.debug("Request rejected because one or more fields failed validation: {}",
                exception.getBindingResult().getFieldErrors().stream()
                        .map(FieldError::getField)
                        .distinct()
                        .toList());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflict() {
        logger.warn("Request rejected because it conflicts with existing PMS data");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The request conflicts with existing PMS data");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
