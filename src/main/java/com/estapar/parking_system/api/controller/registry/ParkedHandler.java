package com.estapar.parking_system.api.controller.registry;

import com.estapar.parking_system.api.controller.contract.WebhookHandler;
import com.estapar.parking_system.api.dto.WebhookDtos.ParkedEvent;
import com.estapar.parking_system.application.service.SessionAppService;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class ParkedHandler implements WebhookHandler<ParkedEvent> {
    private final SessionAppService service;

    public Class<ParkedEvent> supports() {
        return ParkedEvent.class;
    }

    public void handle(ParkedEvent parkedEvent) {
        service.handleParked(parkedEvent);
    }
}