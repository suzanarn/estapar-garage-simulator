package com.estapar.parking_system.application.service;

import com.estapar.parking_system.api.dto.WebhookDtos;
import com.estapar.parking_system.application.helpers.EntryAllocator;
import com.estapar.parking_system.application.helpers.ParkingPreemption;
import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import com.estapar.parking_system.domain.entity.VehicleSessionEntity;
import com.estapar.parking_system.domain.exceptions.GarageFullException;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import com.estapar.parking_system.domain.service.DynamicFactorService;
import com.estapar.parking_system.domain.service.OccupancyService;
import com.estapar.parking_system.domain.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class SessionAppServiceTest {

    @Mock VehicleSessionRepository sessionRepo;
    @Mock OccupancyService occupancyService;
    @Mock DynamicFactorService dynamicFactorService;
    @Mock SpotRepository spotRepo;
    @Mock SectorRepository sectorRepo;
    @Mock PricingService pricingService;
    @Mock EntryAllocator entryAllocator;
    @Mock ParkingPreemption preemption;

    SessionAppService service;

    @BeforeEach
    void setUp() {
        service = new SessionAppService(
                sessionRepo, occupancyService, dynamicFactorService,
                spotRepo, sectorRepo, pricingService,
                entryAllocator, preemption
        );
    }


    @Test
    void handleEntry_success_reserves_spot_and_sets_sector_basePrice() {
        var evt = new WebhookDtos.EntryEvent("AAA1234", "2025-01-01T12:00:00Z", WebhookDtos.EventType.ENTRY);

        when(sessionRepo.countOpenByPlate("AAA1234")).thenReturn(0L);

        when(occupancyService.globalRatioBySpots()).thenReturn(new BigDecimal("0.10"));
        when(dynamicFactorService.compute(new BigDecimal("0.10"))).thenReturn(new BigDecimal("0.90"));

        // 1º save retorna sessão com ID gerado
        var saved = new VehicleSessionEntity();
        saved.setId(77L);
        when(sessionRepo.save(any(VehicleSessionEntity.class))).thenAnswer(inv -> {
            VehicleSessionEntity s = inv.getArgument(0);
            if (s.getId() == null) return saved; // primeira persistência (gera ID)
            return s; // demais
        });

        var sec = sector(1L, "A", new BigDecimal("40.50"));
        var spot = spot(10L, null, sec);
        when(entryAllocator.allocateForSession(77L)).thenReturn(new EntryAllocator.Allocation(sec, spot));

        service.handleEntry(evt);

        verify(sessionRepo, atLeast(2)).save(any(VehicleSessionEntity.class));
        verify(sessionRepo, never()).deleteById(anyLong());
    }

    @Test
    void handleEntry_duplicate_plate_is_ignored() {
        var evt = new WebhookDtos.EntryEvent("AAA1234", "2025-01-01T12:00:00Z", WebhookDtos.EventType.ENTRY);
        when(sessionRepo.countOpenByPlate("AAA1234")).thenReturn(1L);

        service.handleEntry(evt);

        verifyNoInteractions(entryAllocator);
        verify(sessionRepo, never()).save(any());
    }

    @Test
    void handleEntry_no_spot_available_deletes_session_and_throws() {
        var evt = new WebhookDtos.EntryEvent("AAA1234", "2025-01-01T12:00:00Z", WebhookDtos.EventType.ENTRY);

        when(sessionRepo.countOpenByPlate("AAA1234")).thenReturn(0L);
        when(occupancyService.globalRatioBySpots()).thenReturn(new BigDecimal("0.75"));
        when(dynamicFactorService.compute(new BigDecimal("0.75"))).thenReturn(new BigDecimal("1.10"));

        var persisted = new VehicleSessionEntity(); persisted.setId(999L);
        when(sessionRepo.save(any(VehicleSessionEntity.class))).thenReturn(persisted);

        when(entryAllocator.allocateForSession(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.handleEntry(evt))
                .isInstanceOf(GarageFullException.class)
                .hasMessageContaining("Garage is full");

        verify(sessionRepo).deleteById(999L);
    }

    @Test
    void handleParked_delegates_to_preemption_and_persists_on_success() {
        var event = new WebhookDtos.ParkedEvent("AAA1234", new BigDecimal("1.0"), new BigDecimal("2.0"), WebhookDtos.EventType.PARKED);

        var sess = new VehicleSessionEntity(); sess.setId(10L);
        when(sessionRepo.findTopByLicensePlateAndExitTimeIsNullOrderByIdDesc("AAA1234"))
                .thenReturn(Optional.of(sess));

        var sec = sector(1L, "A", new BigDecimal("40.50"));
        var dest = spot(200L, null, sec);
        when(spotRepo.findAllByLatAndLngOrderByIdAsc(new BigDecimal("1.0"), new BigDecimal("2.0")))
                .thenReturn(List.of(dest));

        when(preemption.placeOrPreempt(sess, dest))
                .thenReturn(ParkingPreemption.Result.PREEMPTED);

        service.handleParked(event);

        verify(preemption).placeOrPreempt(sess, dest);
        verify(sessionRepo).save(sess);
    }

    @Test
    void handleParked_no_session_open_returns_gracefully() {
        var event = new WebhookDtos.ParkedEvent("AAA1234", new BigDecimal("1.0"), new BigDecimal("2.0"), WebhookDtos.EventType.PARKED);
        when(sessionRepo.findTopByLicensePlateAndExitTimeIsNullOrderByIdDesc("AAA1234"))
                .thenReturn(Optional.empty());

        service.handleParked(event);

        verifyNoInteractions(preemption);
 // TODO: VERIFY        verifyNoInteractions(sessionRepo);
    }

    @Test
    void handleParked_uses_nearest_fallback_when_exact_not_found_and_no_sector_then_global() {
        var event = new WebhookDtos.ParkedEvent("AAA1234", new BigDecimal("9.9"), new BigDecimal("8.8"), WebhookDtos.EventType.PARKED);

        var sess = new VehicleSessionEntity(); sess.setId(10L);
        // sem setor definido ainda
        sess.setSector(null);
        when(sessionRepo.findTopByLicensePlateAndExitTimeIsNullOrderByIdDesc("AAA1234"))
                .thenReturn(Optional.of(sess));

        when(spotRepo.findAllByLatAndLngOrderByIdAsc(new BigDecimal("9.9"), new BigDecimal("8.8")))
                .thenReturn(List.of());

        var sec = sector(2L, "B", new BigDecimal("4.10"));
        var dest = spot(22L, null, sec);
        when(spotRepo.findNearestGlobal(new BigDecimal("9.9"), new BigDecimal("8.8")))
                .thenReturn(Optional.of(dest));

        when(preemption.placeOrPreempt(sess, dest)).thenReturn(ParkingPreemption.Result.PLACED_FREE);

        service.handleParked(event);

        verify(preemption).placeOrPreempt(sess, dest);
        verify(sessionRepo).save(sess);
    }

    @Test
    void handleParked_denied_does_not_persist_session() {
        var event = new WebhookDtos.ParkedEvent("AAA1234", new BigDecimal("1.0"), new BigDecimal("2.0"), WebhookDtos.EventType.PARKED);

        var sess = new VehicleSessionEntity(); sess.setId(10L);
        when(sessionRepo.findTopByLicensePlateAndExitTimeIsNullOrderByIdDesc("AAA1234"))
                .thenReturn(Optional.of(sess));

        var sec = sector(1L, "A", new BigDecimal("40.50"));
        var dest = spot(200L, null, sec);
        when(spotRepo.findAllByLatAndLngOrderByIdAsc(new BigDecimal("1.0"), new BigDecimal("2.0")))
                .thenReturn(List.of(dest));

        when(preemption.placeOrPreempt(sess, dest))
                .thenReturn(ParkingPreemption.Result.DENIED);

        service.handleParked(event);

        verify(preemption).placeOrPreempt(sess, dest);
        verify(sessionRepo, never()).save(sess);
    }


    @Test
    void handleExit_computes_amount_and_frees_spot() {
        var event = new WebhookDtos.ExitEvent("AAA1234", "2025-01-01T13:00:00Z", WebhookDtos.EventType.EXIT);

        var sec = sector(1L, "A", new BigDecimal("40.50"));
        var spot = spot(10L, 77L, sec);

        var sess = new VehicleSessionEntity();
        sess.setId(77L);
        sess.setLicensePlate("AAA1234");
        sess.setEntryTime(Instant.parse("2025-01-01T12:00:00Z"));
        sess.setSector(sec);
        sess.setBasePrice(new BigDecimal("40.50"));
        sess.setPriceFactor(new BigDecimal("1.10"));
        sess.setSpot(spot);

        when(sessionRepo.findTopByLicensePlateAndExitTimeIsNullOrderByIdDesc("AAA1234"))
                .thenReturn(Optional.of(sess));

        when(pricingService.hourlyCharge(
                new BigDecimal("40.50"),
                new BigDecimal("1.10"),
                Instant.parse("2025-01-01T12:00:00Z"),
                Instant.parse("2025-01-01T13:00:00Z")
        )).thenReturn(new BigDecimal("44.55"));

        service.handleExit(event);

        assertThat(spot.getOccupiedBySessionId()).isNull();
        verify(spotRepo).save(argThat(s -> s.getId().equals(10L) && s.getOccupiedBySessionId() == null));
        assertThat(sess.getChargedAmount()).isEqualByComparingTo("44.55");
        verify(sessionRepo, atLeastOnce()).save(sess);
    }

    @Test
    void handleExit_without_basePrice_uses_min_sector_basePrice() {
        var event = new WebhookDtos.ExitEvent("AAA1234", "2025-01-01T12:10:00Z", WebhookDtos.EventType.EXIT);

        var sess = new VehicleSessionEntity();
        sess.setId(1L);
        sess.setLicensePlate("AAA1234");
        sess.setEntryTime(Instant.parse("2025-01-01T12:00:00Z"));
        sess.setPriceFactor(new BigDecimal("1.00"));
        sess.setBasePrice(null);
        sess.setSector(null);

        when(sessionRepo.findTopByLicensePlateAndExitTimeIsNullOrderByIdDesc("AAA1234"))
                .thenReturn(Optional.of(sess));
        when(sectorRepo.findMinBasePrice()).thenReturn(new BigDecimal("4.10"));

        when(pricingService.hourlyCharge(
                new BigDecimal("4.10"),
                new BigDecimal("1.00"),
                Instant.parse("2025-01-01T12:00:00Z"),
                Instant.parse("2025-01-01T12:10:00Z")
        )).thenReturn(BigDecimal.ZERO); // < 30min grátis

        service.handleExit(event);

        assertThat(sess.getBasePrice()).isEqualByComparingTo("4.10");
        verify(sessionRepo, atLeastOnce()).save(sess);
    }


    private static SectorEntity sector(long id, String code, BigDecimal basePrice) {
        var s = new SectorEntity();
        s.setId(id); s.setCode(code); s.setBasePrice(basePrice);
        s.setMaxCapacity(10);
        s.setOpenHour("00:00"); s.setCloseHour("23:59");
        s.setDurationLimitMinutes(1440);
        return s;
    }

    private static SpotEntity spot(long id, Long occupiedBy, SectorEntity sector) {
        var sp = new SpotEntity();
        sp.setId(id);
        sp.setSector(sector);
        sp.setOccupiedBySessionId(occupiedBy);
        return sp;
    }
}
