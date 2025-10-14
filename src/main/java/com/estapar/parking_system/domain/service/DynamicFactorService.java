package com.estapar.parking_system.domain.service;

import java.math.BigDecimal;

public class DynamicFactorService {
    public BigDecimal compute(BigDecimal ratio) {
        if (ratio.compareTo(new BigDecimal("0.25")) < 0) return new BigDecimal("0.90");
        if (ratio.compareTo(new BigDecimal("0.50")) <= 0) return BigDecimal.ONE;
        if (ratio.compareTo(new BigDecimal("0.75")) <= 0) return new BigDecimal("1.10");
        return new BigDecimal("1.25");
    }
}
