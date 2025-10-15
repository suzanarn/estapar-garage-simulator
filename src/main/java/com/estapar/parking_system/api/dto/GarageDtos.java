package com.estapar.parking_system.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class GarageDtos {
    public record GarageResponse(List<SectorDto> garage, List<SpotDto> spots) {}

    public record SectorDto(
            @JsonProperty("sector") String sector,
            @JsonProperty("base_price") BigDecimal basePrice,
            @JsonProperty("max_capacity") Integer maxCapacity,
            @JsonProperty("open_hour") String openHour,
            @JsonProperty("close_hour") String closeHour,
            @JsonProperty("duration_limit_minutes") Integer durationLimitMinutes
    ) {}

    public record SpotDto(
            @JsonProperty("id") Long id,
            @JsonProperty("sector") String sector,
            @JsonProperty("lat") BigDecimal lat,
            @JsonProperty("lng") BigDecimal lng,
            @JsonProperty("occupied") Boolean occupied
    ) {}
}
