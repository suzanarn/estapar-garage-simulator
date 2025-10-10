package com.estapar.parking_system.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class GarageDtos {
    public record GarageResponse(List<SectorDto> garage, List<SpotDto> spots) {}

    public record SectorDto(
            String sector,
            BigDecimal base_price,
            Integer max_capacity,
            String open_hour,
            String close_hour,
            Integer duration_limit_minutes
    ) {}

    public record SpotDto(
            Long id,
            String sector,
            BigDecimal lat,
            BigDecimal lng,
            Boolean occupied
    ) {}
}
