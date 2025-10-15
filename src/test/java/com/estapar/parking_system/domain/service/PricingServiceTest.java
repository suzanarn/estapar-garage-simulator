package com.estapar.parking_system.domain.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    private final PricingService service = new PricingService();

    @Test
    void shouldReturnZeroWhenDurationIs30MinutesOrLess() {
        Instant entry = Instant.parse("2025-01-01T12:00:00Z");
        Instant exit = Instant.parse("2025-01-01T12:25:00Z");

        BigDecimal result = service.hourlyCharge(BigDecimal.TEN, BigDecimal.ONE, entry, exit);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldChargeBasePriceForFirstHourAfter30Minutes() {
        Instant entry = Instant.parse("2025-01-01T12:00:00Z");
        Instant exit = Instant.parse("2025-01-01T12:59:59Z");

        BigDecimal result = service.hourlyCharge(BigDecimal.TEN, BigDecimal.ONE, entry, exit);
        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    void shouldRoundUpToNextHourWhenBeyond60Minutes() {
        Instant entry = Instant.parse("2025-01-01T12:00:00Z");
        Instant exit = Instant.parse("2025-01-01T13:10:00Z");

        BigDecimal result = service.hourlyCharge(BigDecimal.TEN, BigDecimal.ONE, entry, exit);
        assertEquals(new BigDecimal("20.00"), result);
    }

    @Test
    void shouldApplyDynamicFactorCorrectly() {
        Instant entry = Instant.parse("2025-01-01T12:00:00Z");
        Instant exit = Instant.parse("2025-01-01T14:00:00Z");

        BigDecimal result = service.hourlyCharge(new BigDecimal("10.00"), new BigDecimal("1.10"), entry, exit);
        assertEquals(new BigDecimal("22.00"), result);
    }
}
