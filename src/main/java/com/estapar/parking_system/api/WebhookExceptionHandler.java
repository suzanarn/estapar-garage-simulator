package com.estapar.parking_system.api;

import com.estapar.parking_system.domain.exceptions.GarageFullException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class WebhookExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(WebhookExceptionHandler.class);

    @ExceptionHandler(GarageFullException.class)
    ResponseEntity<Void> onGarageFull(GarageFullException ex) {
        log.warn("ENTRY ignored: {}", ex.getMessage());
        return ResponseEntity.ok().build(); // simulador espera 200
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Void> onBadPayload(IllegalArgumentException ex) {
        // payload inválido: ainda assim 200 para o simulador, mas log visível
        log.warn("Webhook payload invalid: {}", ex.getMessage());
        return ResponseEntity.ok().build();
    }
}

