package com.estapar.parking_system.domain.service;


import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class DynamicFactorServiceTest {

    private final DynamicFactorService service = new DynamicFactorService();

    @Test
    void shouldApply10PercentDiscountWhenOccupancyBelow25() {
        BigDecimal ratio = new BigDecimal("0.20");
        assertEquals(new BigDecimal("0.90"), service.compute(ratio));
    }

    @Test
    void shouldApplyNormalPriceWhenOccupancyUpTo50() {
        BigDecimal ratio = new BigDecimal("0.50");
        assertEquals(BigDecimal.ONE, service.compute(ratio));
    }

    @Test
    void shouldApply10PercentIncreaseWhenOccupancyUpTo75() {
        BigDecimal ratio = new BigDecimal("0.70");
        assertEquals(new BigDecimal("1.10"), service.compute(ratio));
    }

    @Test
    void shouldApply25PercentIncreaseWhenOccupancyAbove75() {
        BigDecimal ratio = new BigDecimal("0.95");
        assertEquals(new BigDecimal("1.25"), service.compute(ratio));
    }
}
