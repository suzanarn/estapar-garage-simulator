package com.estapar.parking_system.application.helpers;


import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.service.OccupancyService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EntryAllocatorTest {

    SectorRepository sectorRepo = mock(SectorRepository.class);
    SpotRepository spotRepo = mock(SpotRepository.class);
    OccupancyService occupancy = mock(OccupancyService.class);

    EntryAllocator allocator = new EntryAllocator(sectorRepo, spotRepo, occupancy);

    @Test
    void allocate_ok_first_try() {
        var sector = new SectorEntity(); sector.setId(10L);
        var spot = new SpotEntity(); spot.setId(100L);

        when(sectorRepo.findAll()).thenReturn(java.util.List.of(sector));
        when(occupancy.isSectorFull(10L)).thenReturn(false);
        when(spotRepo.findFirstFreeInSector(10L)).thenReturn(Optional.of(spot));
        when(spotRepo.tryOccupy(100L, 1L)).thenReturn(1);

        var res = allocator.allocateForSession(1L);

        assertThat(res).isNotNull();
        assertThat(res.sector().getId()).isEqualTo(10L);
        assertThat(res.spot().getId()).isEqualTo(100L);
    }

    @Test
    void allocate_race_then_success_on_next_free() {
        var sector = new SectorEntity(); sector.setId(10L);
        var spot1 = new SpotEntity(); spot1.setId(101L);
        var spot2 = new SpotEntity(); spot2.setId(102L);

        when(sectorRepo.findAll()).thenReturn(java.util.List.of(sector));
        when(occupancy.isSectorFull(10L)).thenReturn(false);
        // 1ª tentativa retorna spot1 (corrida perdida), 2ª tentativa retorna spot2 (vence)
        when(spotRepo.findFirstFreeInSector(10L))
                .thenReturn(Optional.of(spot1))
                .thenReturn(Optional.of(spot2));
        when(spotRepo.tryOccupy(101L, 1L)).thenReturn(0); // perdeu corrida
        when(spotRepo.tryOccupy(102L, 1L)).thenReturn(1); // conseguiu

        var res = allocator.allocateForSession(1L);

        assertThat(res).isNotNull();
        assertThat(res.spot().getId()).isEqualTo(102L);
    }

    @Test
    void allocate_none_returns_null() {
        var sector = new SectorEntity(); sector.setId(10L);
        when(sectorRepo.findAll()).thenReturn(java.util.List.of(sector));
        when(occupancy.isSectorFull(10L)).thenReturn(false);
        when(spotRepo.findFirstFreeInSector(10L)).thenReturn(Optional.empty());

        var res = allocator.allocateForSession(1L);
        assertThat(res).isNull();
    }
}

