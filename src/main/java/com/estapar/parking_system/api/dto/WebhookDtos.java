package com.estapar.parking_system.api.dto;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class WebhookDtos {

    public enum EventType { ENTRY, PARKED, EXIT }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "event_type",
            visible = true
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = EntryEvent.class,  name = "ENTRY"),
            @JsonSubTypes.Type(value = ParkedEvent.class, name = "PARKED"),
            @JsonSubTypes.Type(value = ExitEvent.class,   name = "EXIT")
    })
    public sealed interface WebhookEvent permits EntryEvent, ParkedEvent, ExitEvent {
        @JsonProperty("event_type")
        EventType eventType();

        @JsonProperty("license_plate")
        String licensePlate();
    }

    public record EntryEvent(
            @NotBlank
            @JsonProperty("license_plate")
            String licensePlate,

            @NotBlank
            @JsonProperty("entry_time")
            String entryTime,

            @NotNull
            @JsonProperty("event_type")
            EventType eventType
    ) implements WebhookEvent {}

    public record ParkedEvent(
            @NotBlank
            @JsonProperty("license_plate")
            String licensePlate,

            @NotNull
            @JsonProperty("lat")
            BigDecimal lat,

            @NotNull
            @JsonProperty("lng")
            BigDecimal lng,

            @NotNull
            @JsonProperty("event_type")
            EventType eventType
    ) implements WebhookEvent {}

    public record ExitEvent(
            @NotBlank
            @JsonProperty("license_plate")
            String licensePlate,

            @NotBlank
            @JsonProperty("exit_time")
            String exitTime,

            @NotNull
            @JsonProperty("event_type")
            EventType eventType
    ) implements WebhookEvent {}
}
