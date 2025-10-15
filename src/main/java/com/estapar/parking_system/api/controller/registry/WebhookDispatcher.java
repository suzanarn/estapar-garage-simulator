package com.estapar.parking_system.api.controller.registry;

import com.estapar.parking_system.api.controller.contract.WebhookHandler;
import com.estapar.parking_system.api.dto.WebhookDtos.WebhookEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**Registry pattern to deal with the type of request**/
@Component
public class WebhookDispatcher {
    private final Map<Class<?>, WebhookHandler<?>> byType;
    WebhookDispatcher(List<WebhookHandler<?>> handlers) {
        this.byType = handlers.stream().collect(
                java.util.stream.Collectors.toMap(WebhookHandler::supports, h -> h));
    }
    @SuppressWarnings("unchecked")
    public <T extends WebhookEvent> void dispatch(T ev) {
        var h = (WebhookHandler<T>) byType.get(ev.getClass());
        if (h == null) throw new IllegalArgumentException("No handler for " + ev.getClass());
        h.handle(ev);
    }
}