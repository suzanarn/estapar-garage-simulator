package com.estapar.parking_system.api.controller.contract;

import com.estapar.parking_system.api.dto.WebhookDtos.WebhookEvent;

public interface WebhookHandler<T extends WebhookEvent> {
    Class<T> supports();
    void handle(T event);
}
