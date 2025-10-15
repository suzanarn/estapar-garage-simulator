package com.estapar.parking_system.api.controller.registry;

import com.estapar.parking_system.api.controller.contract.WebhookHandler;
import com.estapar.parking_system.api.dto.WebhookDtos.EntryEvent;

import com.estapar.parking_system.application.service.SessionAppService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class EntryHandler implements WebhookHandler<EntryEvent> {
    private final SessionAppService service;
    public Class<EntryEvent> supports() {
        return EntryEvent.class;
    }

    public void handle(EntryEvent entryEvent) {
        service.handleEntry(entryEvent);
    }
}

