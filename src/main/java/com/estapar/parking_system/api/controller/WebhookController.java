package com.estapar.parking_system.api.controller;

import com.estapar.parking_system.application.service.SessionAppService;
import com.estapar.parking_system.api.dto.WebhookDtos.WebhookEvent;
import com.estapar.parking_system.api.dto.WebhookDtos.EntryEvent;
import com.estapar.parking_system.api.dto.WebhookDtos.ParkedEvent;
import com.estapar.parking_system.api.dto.WebhookDtos.ExitEvent;


import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final SessionAppService service;

    @PostMapping
    public ResponseEntity<Void> receive(@Valid @RequestBody WebhookEvent event) {
        log.info("Recebendo request do webhook. Data info: {}", event);

        switch (event) {
            case EntryEvent entry  -> service.handleEntry(entry);
            case ParkedEvent parked -> service.handleParked(parked);
            case ExitEvent exit   -> service.handleExit(exit);
        }
        return ResponseEntity.ok().build();
    }
}

