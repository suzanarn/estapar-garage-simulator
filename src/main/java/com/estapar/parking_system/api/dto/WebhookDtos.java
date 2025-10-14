package com.estapar.parking_system.api.dto;

import java.math.BigDecimal;

public class WebhookDtos {

    public enum EventType { ENTRY, PARKED, EXIT }

    public interface WebhookEvent {
        EventType event_type();
        String license_plate();
    }

    public record EntryEvent(
            String license_plate,
            String entry_time,      // ISO 8601
            EventType event_type
    ) implements WebhookEvent {}

    public record ParkedEvent(
            String license_plate,
            BigDecimal lat,
            BigDecimal lng,
            EventType event_type
    ) implements WebhookEvent {}

    public record ExitEvent(
            String license_plate,
            String exit_time,
            EventType event_type
    ) implements WebhookEvent {}
}
