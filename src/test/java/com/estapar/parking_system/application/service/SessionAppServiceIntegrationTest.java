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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionAppServiceTest {

    VehicleSessionRepository sessionRepo = mock(VehicleSessionRepository.class);
    OccupancyService occupancyService = mock(OccupancyService.class);
    DynamicFactorService dynamicFactorService = mock(DynamicFactorService.class);
    SpotRepository spotRepository = mock(SpotRepository.class);
    SectorRepository sectorRepository = mock(SectorRepository.class);
    PricingService pricingService = mock(PricingService.class);

    EntryAllocator allocator = mock(EntryAllocator.class);
    ParkingPreemption preemption = mock(ParkingPreemption.class);

    SessionAppService service = new SessionAppService(
            sessionRepo, occupancyService, dynamicFactorService,
            spotRepository, sectorRepository, pricingService,
            allocator, preemption
    );

    @Test
    void handleEntry_ok_sets_sector_baseprice_spot() {
        var event = new WebhookDtos.EntryEvent("AAA1234", "2025-01-01T12:00:00Z", WebhookDtos.EventType.ENTRY);
        when(sessionRepo.findOpenByPlate("AAA1234")).thenReturn(Optional.empty());
        when(dynamicFactorService.compute(any())).thenReturn(new BigDecimal("1.00"));

        var saved = new VehicleSessionEntity(); saved.setId(10L); // retorno do primeiro save
        when(sessionRepo.save(any(VehicleSessionEntity.class))).thenReturn(saved);

        var sector = new SectorEntity(); sector.setId(1L); sector.setBasePrice(new BigDecimal("10.00"));
        var spot = new SpotEntity(); spot.setId(100L); spot.setSector(sector);

        when(allocator.allocateForSession(10L))
                .thenReturn(new EntryAllocator.Allocation(sector, spot));

        service.handleEntry(event);

        // Captura TODAS as chamadas e valida o ÚLTIMO valor salvo
        var captor = ArgumentCaptor.forClass(VehicleSessionEntity.class);
        verify(sessionRepo, atLeast(2)).save(captor.capture());

        var lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);

        assertThat(lastSaved.getId()).isEqualTo(10L);
        assertThat(lastSaved.getSector()).isSameAs(sector);
        assertThat(lastSaved.getBasePrice()).isEqualByComparingTo("10.00");
        assertThat(lastSaved.getSpot()).isSameAs(spot);
    }


    @Test
    void handleEntry_no_spot_deletes_session_and_throws() {
        var event = new WebhookDtos.EntryEvent("AAA1234", "2025-01-01T12:00:00Z", WebhookDtos.EventType.ENTRY);
        when(sessionRepo.findOpenByPlate("AAA1234")).thenReturn(Optional.empty());
        when(dynamicFactorService.compute(any())).thenReturn(new BigDecimal("1.00"));
        var saved = new VehicleSessionEntity(); saved.setId(10L);
        when(sessionRepo.save(any(VehicleSessionEntity.class))).thenReturn(saved);
        when(allocator.allocateForSession(10L)).thenReturn(null);

        assertThatThrownBy(() -> service.handleEntry(event))
                .isInstanceOf(GarageFullException.class);

        verify(sessionRepo).deleteById(10L);
    }

    @Test
    void handleParked_delegates_to_preemption_and_persists_on_success() {
        var event = new WebhookDtos.ParkedEvent("AAA1234", new BigDecimal("1.0"), new BigDecimal("2.0"), WebhookDtos.EventType.PARKED);
        var sess = new VehicleSessionEntity(); sess.setId(10L);
        when(sessionRepo.findOpenByPlate("AAA1234")).thenReturn(Optional.of(sess));

        var sector = new SectorEntity(); sector.setId(1L);
        var dest = new SpotEntity(); dest.setId(200L); dest.setSector(sector);
        when(spotRepository.findByLatAndLng(new BigDecimal("1.0"), new BigDecimal("2.0"))).thenReturn(Optional.of(dest));

        when(preemption.placeOrPreempt(sess, dest)).thenReturn(ParkingPreemption.Result.PREEMPTED);

        service.handleParked(event);

        verify(preemption).placeOrPreempt(sess, dest);
        verify(sessionRepo).save(sess);
    }

    @Test
    void handleParked_no_open_session_noop() {
        var event = new WebhookDtos.ParkedEvent("AAA1234", new BigDecimal("1.0"), new BigDecimal("2.0"), WebhookDtos.EventType.PARKED);
        when(sessionRepo.findOpenByPlate("AAA1234")).thenReturn(Optional.empty());
        service.handleParked(event);
        verifyNoInteractions(preemption);
    }
}
