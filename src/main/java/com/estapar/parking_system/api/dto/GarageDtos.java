package com.estapar.parking_system.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class GarageDtos {
    public record GarageResponse(List<SectorDto> garage, List<SpotDto> spots) {}

    public record SectorDto(
            String sector,
            BigDecimal basePrice,
            Integer maxCapacity,
            String openHour,
            String closeHour,
            Integer durationLimitMinutes
    ) {}

    public record SpotDto(
            Long id,
            String sector,
            BigDecimal lat,
            BigDecimal lng,
            Boolean occupied
    ) {}
}
