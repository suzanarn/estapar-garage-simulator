package com.estapar.parking_system.application.service;

import com.estapar.parking_system.api.dto.WebhookDtos.EntryEvent;
import com.estapar.parking_system.api.dto.WebhookDtos.ParkedEvent;
import com.estapar.parking_system.api.dto.WebhookDtos.ExitEvent;

import com.estapar.parking_system.application.helpers.EntryAllocator;
import com.estapar.parking_system.application.helpers.ParkingPreemption;
import com.estapar.parking_system.application.helpers.TimeParser;
import com.estapar.parking_system.domain.entity.VehicleSessionEntity;
import com.estapar.parking_system.domain.exceptions.GarageFullException;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import com.estapar.parking_system.domain.service.DynamicFactorService;
import com.estapar.parking_system.domain.service.OccupancyService;
import com.estapar.parking_system.domain.service.PricingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Service
@AllArgsConstructor
@Slf4j
public class SessionAppService {
    private final VehicleSessionRepository sessionRepo;
    private final OccupancyService occupancyService;
    private final DynamicFactorService dynamicFactorService;
    private final SpotRepository spotRepository;
    private final SectorRepository sectorRepository;
    private final PricingService pricingService;

    private final EntryAllocator entryAllocator;
    private final ParkingPreemption preemption;

    @Transactional
    public void handleEntry(EntryEvent event) {
        log.info("ENTRY handled for {}", event.licensePlate());
        if (sessionRepo.findOpenByPlate(event.licensePlate()).isPresent()) return;

        BigDecimal factor = dynamicFactorService.compute(occupancyService.globalRatioBySpots());
        VehicleSessionEntity session = sessionRepo.save(VehicleSessionEntity.builder()
                .licensePlate(event.licensePlate())
                .entryTime(TimeParser.parseInstantSafe(event.entryTime()))
                .priceFactor(factor)
                .build());


        var allocation = entryAllocator.allocateForSession(session.getId());
        if (allocation == null) {
            sessionRepo.deleteById(session.getId());
            throw new GarageFullException("Garage is full");
        }

        session.setSector(allocation.sector());
        session.setBasePrice(allocation.sector().getBasePrice());
        session.setSpot(allocation.spot());
        sessionRepo.save(session);
        log.info("ENTRY reserved spot={} sector={} for plate={}",
                allocation.spot().getId(), allocation.sector().getCode(), session.getLicensePlate());
    }

    @Transactional
    public void handleParked(ParkedEvent ev) {
        var session = sessionRepo.findOpenByPlate(ev.licensePlate()).orElse(null);
        if (session == null) { log.debug("PARKED ignored: no open session for {}", ev.licensePlate()); return; }

        var dest = spotRepository.findByLatAndLng(ev.lat(), ev.lng()).orElse(null);
        if (dest == null) return;

        var result = preemption.placeOrPreempt(session, dest);
        switch (result) {
            case NOOP -> { /* nada */ }
            case PLACED_FREE, PREEMPTED -> {
                // garantir persistência das alterações feitas pelo helper
                sessionRepo.save(session);
                if (session.getSpot() != null && !session.getSpot().getId().equals(dest.getId())) {
                    // helpers já mexem em spots; se precisar, ajuste aqui
                }
                log.info("PARKED result={} plate={} spot={}", result, session.getLicensePlate(), dest.getId());
            }
            case DENIED -> log.debug("PARKED denied: plate={} dest={}", session.getLicensePlate(), dest.getId());
        }
    }

    @Transactional
    public void handleExit(ExitEvent ev) {
        var session = sessionRepo.findOpenByPlate(ev.licensePlate()).orElse(null);
        if (session == null) return;

        Instant exit = TimeParser.parseInstantSafe(ev.exitTime());
        session.setExitTime(exit);

        if (session.getSector() != null) {
            long stayed = Duration.between(session.getEntryTime(), exit).toMinutes();
            Integer limit = session.getSector().getDurationLimitMinutes();
            if (limit != null && stayed > limit) {
                log.warn("Duration limit exceeded: plate={}, sector={}, stayed={}min, limit={}min",
                        session.getLicensePlate(), session.getSector().getCode(), stayed, limit);
            }
        }

        if (session.getBasePrice() == null) {
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

        var spot = session.getSpot();
        if (spot != null && session.getId().equals(spot.getOccupiedBySessionId())) {
            spot.setOccupiedBySessionId(null);
            spotRepository.save(spot);
            session.setSpot(null);
            sessionRepo.save(session);
        }
    }

}
