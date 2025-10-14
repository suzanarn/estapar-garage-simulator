package com.estapar.parking_system.api.controller;

import com.estapar.parking_system.api.dto.WebhookDtos;
import com.estapar.parking_system.application.service.SessionAppService;
import com.estapar.parking_system.api.dto.WebhookDtos.WebhookEvent;
import com.estapar.parking_system.api.dto.WebhookDtos.EntryEvent;
import com.estapar.parking_system.api.dto.WebhookDtos.ParkedEvent;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/webhook")
@AllArgsConstructor
public class WebhookController {
    private final SessionAppService service;

    @PostMapping
    public ResponseEntity<Void> receive(@Valid @RequestBody WebhookEvent event) {
        switch (event) {
            case EntryEvent e  -> service.handleEntry(e);
            case ParkedEvent p -> service.handleParked(p);
            case WebhookDtos.ExitEvent x   -> service.handleExit(x);
        }
        return ResponseEntity.ok().build();
    }
}

