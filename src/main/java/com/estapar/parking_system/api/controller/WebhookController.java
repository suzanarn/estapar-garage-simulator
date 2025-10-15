package com.estapar.parking_system.api.controller;

import com.estapar.parking_system.api.controller.registry.WebhookDispatcher;
import com.estapar.parking_system.api.dto.WebhookDtos.WebhookEvent;


import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/webhook")
@AllArgsConstructor
@Slf4j
public class WebhookController {
    private final WebhookDispatcher dispatcher;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(@Valid @RequestBody WebhookEvent event) {
        log.info("Getting request from webhook. Data info: {}", event);
        dispatcher.dispatch(event);
        return ResponseEntity.ok().build();
    }
}

