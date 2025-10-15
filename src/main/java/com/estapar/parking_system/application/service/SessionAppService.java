package com.estapar.parking_system.application.service;

import com.estapar.parking_system.api.dto.WebhookDtos.EntryEvent;
import com.estapar.parking_system.api.dto.WebhookDtos.ParkedEvent;
import com.estapar.parking_system.api.dto.WebhookDtos.ExitEvent;

import com.estapar.parking_system.application.helpers.EntryAllocator;
import com.estapar.parking_system.application.helpers.ParkingPreemption;
import com.estapar.parking_system.application.helpers.TimeParser;
import com.estapar.parking_system.domain.entity.SpotEntity;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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

            if (sessionRepo.countOpenByPlate(event.licensePlate()) > 0) {
                log.debug("ENTRY ignored: already open for plate={}", event.licensePlate());
                return;
            }

            BigDecimal factor = dynamicFactorService.compute(occupancyService.globalRatioBySpots());
            VehicleSessionEntity session = VehicleSessionEntity.builder()
                    .licensePlate(event.licensePlate())
                    .entryTime(TimeParser.parseInstantSafe(event.entryTime()))
                    .priceFactor(factor)
                    .build();

            try {
                session = sessionRepo.save(session);

            } catch (DataIntegrityViolationException dup) {
                log.info("ENTRY duplicate suppressed by unique index for plate={}", event.licensePlate());
                return;
            }

            var allocation = entryAllocator.allocateForSession(session.getId());
            if (allocation == null) {
                sessionRepo.deleteById(session.getId());
                throw new GarageFullException("Garage is full");
            }

            session.setSector(allocation.sector());
            session.setBasePrice(allocation.sector().getBasePrice());
            session.setSpot(allocation.spot());
            sessionRepo.save(session);

            log.info("ENTRY reserved spot={} sector={} for plate={}", allocation.spot().getId(), allocation.sector().getCode(), session.getLicensePlate());
        }

        @Transactional
        public void handleParked(ParkedEvent ev) {
            VehicleSessionEntity session =
                    sessionRepo.findTopByLicensePlateAndExitTimeIsNullOrderByIdDesc(ev.licensePlate()).orElse(null);
            if (session == null) {
                log.debug("PARKED ignored: no open session for {}", ev.licensePlate());
                return;
            }

            List<SpotEntity> candidates = spotRepository.findAllByLatAndLngOrderByIdAsc(ev.lat(), ev.lng());

            SpotEntity destination = null;
            if (!candidates.isEmpty()) {
                destination = candidates.stream()
                        .filter(s -> session.getId().equals(s.getOccupiedBySessionId()))
                        .findFirst()
                        .orElse(candidates.getFirst());
            } else {
                if (session.getSector() != null) {
                    destination = spotRepository.findNearestInSector(session.getSector().getId(), ev.lat(), ev.lng())
                            .orElse(null);
                }
                if (destination == null) {
                    destination = spotRepository.findNearestGlobal(ev.lat(), ev.lng()).orElse(null);
                }
            }

            if (destination == null) {
                log.debug("PARKED ignored: no spot matched (exact/nearest) for plate={}", session.getLicensePlate());
                return;
            }

            var result = preemption.placeOrPreempt(session, destination);

            log.debug("PARKED resolving destination: plate={}, requested=({},{}), chosenSpotId={}, chosenLat={}, chosenLng={}, chosenSector={}",
                    session.getLicensePlate(),
                    ev.lat(), ev.lng(),
                    destination.getId(), destination.getLat(), destination.getLng(),
                    destination.getSector().getCode());


            switch (result) {
                case NOOP -> {/**/}
                case PLACED_FREE, PREEMPTED -> {
                    sessionRepo.save(session);
                    log.info("PARKED result={} plate={} spot={}, base price = {}", result, session.getLicensePlate(), destination.getId(), session.getBasePrice());
                }
                case DENIED -> log.debug("PARKED denied: plate={} spot={}", session.getLicensePlate(), destination.getId());
            }
        }

        @Transactional
        public void handleExit(ExitEvent ev) {
            VehicleSessionEntity session = sessionRepo.findTopByLicensePlateAndExitTimeIsNullOrderByIdDesc(ev.licensePlate()).orElse(null);
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
                BigDecimal minBase = sectorRepository.findMinBasePrice();
                session.setBasePrice(minBase != null ? minBase : BigDecimal.ZERO);
            }

            BigDecimal amount = pricingService.hourlyCharge(
                    session.getBasePrice(),
                    session.getPriceFactor(),
                    session.getEntryTime(),
                    exit
            );
            session.setChargedAmount(amount);
            sessionRepo.save(session);

            SpotEntity spot = session.getSpot();
            if (spot != null && session.getId().equals(spot.getOccupiedBySessionId())) {
                spot.setOccupiedBySessionId(null);
                spotRepository.save(spot);
                session.setSpot(null);
                sessionRepo.save(session);
            }
        }
    }
