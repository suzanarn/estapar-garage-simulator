package com.estapar.parking_system.api.dto;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class WebhookDtos {

    public enum EventType { ENTRY, PARKED, EXIT }

    /**
     * Interface selada: garante que só existam 3 tipos válidos de evento
     * e facilita o uso de pattern matching (switch expressions).
     */
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
    public sealed interface WebhookEvent
            permits EntryEvent, ParkedEvent, ExitEvent {

        EventType event_type();
        String license_plate();
    }

    public record EntryEvent(
            @NotBlank @JsonProperty("license_plate") String license_plate,
            @NotBlank @JsonProperty("entry_time")    String entry_time,
            @NotNull  @JsonProperty("event_type")    EventType event_type
    ) implements WebhookEvent {}

    public record ParkedEvent(
            @NotBlank   @JsonProperty("license_plate") String license_plate,
            @NotNull    @JsonProperty("lat")           BigDecimal lat,
            @NotNull    @JsonProperty("lng")           BigDecimal lng,
            @NotNull    @JsonProperty("event_type")    EventType event_type
    ) implements WebhookEvent {}

    public record ExitEvent(
            @NotBlank @JsonProperty("license_plate") String license_plate,
            @NotBlank @JsonProperty("exit_time")     String exit_time,
            @NotNull  @JsonProperty("event_type")    EventType event_type
    ) implements WebhookEvent {}
}
