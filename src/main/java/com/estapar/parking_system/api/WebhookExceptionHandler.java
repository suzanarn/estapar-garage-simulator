package com.estapar.parking_system.api;

import com.estapar.parking_system.domain.exceptions.GarageFullException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.validation.BindException;

@RestControllerAdvice
class WebhookExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(WebhookExceptionHandler.class);

    @ExceptionHandler(GarageFullException.class)
    ResponseEntity<Void> onGarageFull(GarageFullException ex) {
        log.warn("ENTRY ignored: {}", ex.getMessage());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Void> onBadPayload(IllegalArgumentException ex) {
        log.warn("Webhook payload invalid: {}", ex.getMessage());
        return ResponseEntity.ok().build();
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Void> onNotReadable(HttpMessageNotReadableException ex) {
        Throwable root = ex.getMostSpecificCause();
        if (root instanceof InvalidTypeIdException iti) {
            log.warn("Unknown event_type received: {}", iti.getTypeId());
        } else {
            log.warn("Malformed webhook JSON: {}", ex.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    ResponseEntity<Void> onValidation(Exception ex) {
        log.warn("Webhook validation failed: {}", ex.getMessage());
        return ResponseEntity.ok().build();
    }
}

