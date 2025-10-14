package com.estapar.parking_system.application.helpers;

import java.time.*;
import java.time.format.DateTimeParseException;

public final class TimeParser {
    private TimeParser() {}

    public static Instant parseInstantSafe(String iso) {
        if (iso == null || iso.isBlank()) throw new IllegalArgumentException("Missing timestamp");
        try { return Instant.parse(iso); } catch (DateTimeParseException ignore) { }
        boolean hasOffset = iso.matches(".*[+-]\\d{2}:?\\d{2}$");
        if (!iso.endsWith("Z") && !hasOffset) {
            try { return Instant.parse(iso + "Z"); } catch (DateTimeParseException ignore) { }
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(iso);
            return ldt.toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid timestamp: " + iso, e);
        }
    }
}
