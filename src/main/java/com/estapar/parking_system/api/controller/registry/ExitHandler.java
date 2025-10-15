package com.estapar.parking_system.api.controller.registry;

import com.estapar.parking_system.api.controller.contract.WebhookHandler;
import com.estapar.parking_system.api.dto.WebhookDtos.ExitEvent;
import com.estapar.parking_system.application.service.SessionAppService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ExitHandler implements WebhookHandler<ExitEvent> {
    private final SessionAppService service;
    public Class<ExitEvent> supports() {
        return ExitEvent.class;
    }

    public void handle(ExitEvent exitEvent) {
        service.handleExit(exitEvent);
    }
}

