package com.estapar.parking_system.application.service;

import com.estapar.parking_system.api.dto.RevenueDtos.RevenueResponse;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;

@Service
@RequiredArgsConstructor
public class RevenueService {
    private final VehicleSessionRepository sessionRepo;

    public RevenueResponse revenueForDate(String yyyyMMdd, String sectorCode) {
        LocalDate date = LocalDate.parse(yyyyMMdd);
        Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal sum = sessionRepo.sumChargedBetweenAndSector(start, end,
                (sectorCode == null || sectorCode.isBlank()) ? null : sectorCode);

        sum = (sum == null) ? BigDecimal.ZERO : sum.setScale(2, RoundingMode.HALF_UP);

        return new RevenueResponse(
                sum,
                "BRL",
                Instant.now().toString()
        );
    }
}
