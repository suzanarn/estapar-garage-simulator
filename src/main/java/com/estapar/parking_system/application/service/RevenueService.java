package com.estapar.parking_system.application.service;

import com.estapar.parking_system.api.dto.RevenueDtos.RevenueResponse;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;

import static java.time.temporal.ChronoUnit.MILLIS;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueService {
    private final VehicleSessionRepository sessionRepo;

    public RevenueResponse revenueForDate(String yyyyMMdd, String sectorCode) {
        long t0 = System.nanoTime();

        LocalDate date = LocalDate.parse(yyyyMMdd);
        Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end   = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        String sectorOrNull = (sectorCode == null || sectorCode.isBlank()) ? null : sectorCode;

        log.info("revenue.calc.start date={} start={} end={} sector={}", yyyyMMdd, start, end, sectorOrNull);

        BigDecimal sum = sessionRepo.sumChargedBetweenAndSector(start, end, sectorOrNull);
        sum = (sum == null) ? BigDecimal.ZERO : sum.setScale(2, RoundingMode.HALF_UP);

        RevenueResponse response = new RevenueResponse(sum,
                "BRL",
                Instant.now().truncatedTo(MILLIS).toString());

        long tookMs = (System.nanoTime() - t0) / 1_000_000;
        log.info("revenue.calc.end date={} sector={} amount={} currency={} tookMs={}",
                yyyyMMdd,
                sectorOrNull,
                response.amount(),
                response.currency(),
                tookMs);

        return response;
    }
}
