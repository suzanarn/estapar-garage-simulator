package com.estapar.parking_system.application.service;

import com.estapar.parking_system.api.dto.WebhookDtos.ExitEvent;
import com.estapar.parking_system.api.dto.WebhookDtos.EntryEvent;
import com.estapar.parking_system.api.dto.WebhookDtos.ParkedEvent;
import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.VehicleSessionEntity;
import com.estapar.parking_system.domain.exceptions.GarageFullException;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import com.estapar.parking_system.domain.service.DynamicFactorService;
import com.estapar.parking_system.domain.service.OccupancyService;
import com.estapar.parking_system.domain.service.PricingService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Service
@AllArgsConstructor
public class SessionAppService {
    private final VehicleSessionRepository sessionRepo;
    private final OccupancyService occupancyService;
    private final DynamicFactorService dynamicFactorService;
    private final SpotRepository spotRepository;
    private final SectorRepository sectorRepository;
    private final PricingService pricingService;
    private static final Logger log = LoggerFactory.getLogger(SessionAppService.class);

    @Transactional
    public void handleEntry(EntryEvent event) {
         log.info("ENTRY handled for {}", event.license_plate());
        if (sessionRepo.findOpenByPlate(event.license_plate()).isPresent()) return;

        if (occupancyService.isGarageFullBySessions()) {
            throw new GarageFullException("Garage is full");
        }

        var ratio = occupancyService.globalRatioBySessions();
        var priceFactor = dynamicFactorService.compute(ratio);

        var s = new VehicleSessionEntity();
        s.setLicensePlate(event.license_plate());
        s.setEntryTime(parseInstantSafe(event.entry_time()));
        s.setPriceFactor(priceFactor);
        sessionRepo.save(s);
    }


    @Transactional
    public void handleParked(ParkedEvent ev) {

        var session = sessionRepo.findOpenByPlate(ev.license_plate()).orElse(null);
        if (session == null) { log.debug("PARKED ignored: no open session for {}", ev.license_plate()); return; }

        var spot = spotRepository.findByLatAndLng(ev.lat(), ev.lng()).orElse(null);
        if (spot == null) return;

        SectorEntity sector = spot.getSector();

        long openInSector = sessionRepo.countOpenBySectorId(sector.getId());
        if (openInSector >= sector.getMaxCapacity()) {
             log.debug("PARKED ignored: no open session for {}", ev.license_plate());
            return;
        }

        if (session.getSector() == null) {
            session.setSector(sector);
        }
        if (session.getBasePrice() == null) {
            session.setBasePrice(sector.getBasePrice());
        }

        // Ocupa a vaga se estiver livre
        if (spot.getOccupiedBySessionId() == null) {
            spot.setOccupiedBySessionId(session.getId());
            spotRepository.save(spot);
        }

        // Vincula a sessão ao spot (mesmo se a vaga já estivesse marcada por alguma condição idempotente)
        session.setSpot(spot);
        sessionRepo.save(session);
        log.info("PARKED handled for {} - {}", ev.license_plate(), spot );

    }


    @Transactional
    public void handleExit(ExitEvent ev) {
        log.info("EXIT handled for {} - {}", ev.license_plate(), ev.exit_time() );

        var session = sessionRepo.findOpenByPlate(ev.license_plate()).orElse(null);
        if (session == null) return; // nada a fazer

        Instant exit = parseInstantSafe(ev.exit_time());
        session.setExitTime(exit);

        // Fallback: se nunca PARKED, não temos basePrice/sector.
        if (session.getBasePrice() == null) {
            // escolha coerente e simples: menor basePrice da garagem
            var minBase = sectorRepository.findMinBasePrice();
            session.setBasePrice(minBase != null ? minBase : BigDecimal.ZERO);
        }

        var amount = pricingService.hourlyCharge(
                session.getBasePrice(),
                session.getPriceFactor(),
                session.getEntryTime(),
                exit
        );
        session.setChargedAmount(amount);
        sessionRepo.save(session);

        // Libera a vaga física (se estava ocupada)
        var spot = session.getSpot();
        if (spot != null && session.getId().equals(spot.getOccupiedBySessionId())) {
            spot.setOccupiedBySessionId(null);
            spotRepository.save(spot);
        }
    }




    private Instant parseInstantSafe(String iso) {
        if (iso == null || iso.isBlank()) {
            throw new IllegalArgumentException("Missing timestamp");
        }
        try {
            return Instant.parse(iso);
        } catch (DateTimeParseException ignore) { /* segue abaixo */ }

        boolean hasOffset = iso.matches(".*[+-]\\d{2}:?\\d{2}$");
        if (!iso.endsWith("Z") && !hasOffset) {
            try {
                return Instant.parse(iso + "Z");
            } catch (DateTimeParseException ignore) { /* segue abaixo */ }
        }

        try {
            LocalDateTime ldt = LocalDateTime.parse(iso);
            return ldt.toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid timestamp: " + iso, e);
        }
    }


}
