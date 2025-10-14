package com.estapar.parking_system.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

public class PricingService {
    public BigDecimal hourlyCharge(BigDecimal basePrice,
                                   BigDecimal factor,
                                   Instant entry,
                                   Instant exit) {
        long minutes = Math.max(0, Duration.between(entry, exit).toMinutes());

        if(minutes <= 30) return BigDecimal.ZERO;

        long hours = (long) Math.ceil(minutes/60.0);

        return basePrice.multiply(factor).multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);

    }
}
