package com.estapar.parking_system.api.dto;


import java.math.BigDecimal;

public class RevenueDtos {

    public record RevenueRequest(
            String date,
            String sector
    ) {}

    public record RevenueResponse(
            BigDecimal amount,
            String currency,
            String timestamp
    ) {}
}

