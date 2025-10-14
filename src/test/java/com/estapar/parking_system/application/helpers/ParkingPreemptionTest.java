package com.estapar.parking_system.application.helpers;


import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import com.estapar.parking_system.domain.entity.VehicleSessionEntity;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParkingPreemptionTest {

    SpotRepository spotRepo = mock(SpotRepository.class);
    SectorRepository sectorRepo = mock(SectorRepository.class);
    VehicleSessionRepository vehicleSessionRepository = mock(VehicleSessionRepository.class);
    ParkingPreemption preemption = new ParkingPreemption(spotRepo, sectorRepo, vehicleSessionRepository);

    private static VehicleSessionEntity session(long id) {
        var s = new VehicleSessionEntity();
        s.setId(id);
        return s;
    }
    private static SpotEntity spot(long id, Long occupiedBy, SectorEntity sector) {
        var s = new SpotEntity();
        s.setId(id);
        s.setOccupiedBySessionId(occupiedBy);
        s.setSector(sector);
        return s;
    }
    private static SectorEntity sector(long id) {
        var s = new SectorEntity(); s.setId(id); return s;
    }

    @Test
    void free_destination_tryOccupy_success_places_and_releases_prev() {
        var sec = sector(1L);
        var dest = spot(200L, null, sec);
        var prev = spot(100L, 10L, sec);

        var s = session(10L);
        s.setSpot(prev);

        when(spotRepo.tryOccupy(200L, 10L)).thenReturn(1);

        var r = preemption.placeOrPreempt(s, dest);

        assertThat(r).isEqualTo(ParkingPreemption.Result.PLACED_FREE);
        // prev foi liberada
        verify(spotRepo).save(argThat(p -> p.getId().equals(100L) && p.getOccupiedBySessionId() == null));
        // s agora aponta para dest
        assertThat(s.getSpot()).isEqualTo(dest);
        assertThat(s.getSector()).isEqualTo(sec);
    }

    @Test
    void free_destination_tryOccupy_race_lost_noop() {
        var sec = sector(1L);
        var dest = spot(200L, null, sec);
        var s = session(10L);

        when(spotRepo.tryOccupy(200L, 10L)).thenReturn(0);

        var r = preemption.placeOrPreempt(s, dest);
        assertThat(r).isEqualTo(ParkingPreemption.Result.NOOP);
        assertThat(s.getSpot()).isNull();
    }

    @Test
    void preemption_denied_no_alternative() {
        var sec = sector(1L);
        var dest = spot(200L, 99L, sec); // ocupado por T=99
        var s = session(10L); // S=10

        when(spotRepo.findFirstFreeInSector(1L)).thenReturn(Optional.empty());
        when(sectorRepo.findAll()).thenReturn(java.util.List.of(sec));

        var r = preemption.placeOrPreempt(s, dest);

        assertThat(r).isEqualTo(ParkingPreemption.Result.DENIED);

        // houve leitura…
        verify(spotRepo, atLeastOnce()).findFirstFreeInSector(1L);
        // …mas NÃO houve mutações
        verify(spotRepo, never()).tryOccupy(anyLong(), anyLong());
        verify(spotRepo, never()).trySwapOccupant(anyLong(), anyLong(), anyLong());
        verify(spotRepo, never()).save(any());
    }


    @Test
    void preemption_relocate_then_swap_success() {
        var sec = sector(1L);
        var dest = spot(200L, 99L, sec); // Y ocupado por T=99
        var alt = spot(300L, null, sec); // vaga livre p/ T
        var prev = spot(100L, 10L, sec); // X atual de S=10

        var s = session(10L); s.setSpot(prev);

        when(spotRepo.findFirstFreeInSector(1L)).thenReturn(Optional.of(alt));
        when(spotRepo.tryOccupy(300L, 99L)).thenReturn(1);      // realocar T em alt
        when(spotRepo.trySwapOccupant(200L, 99L, 10L)).thenReturn(1); // swap Y: T->S

        var r = preemption.placeOrPreempt(s, dest);

        assertThat(r).isEqualTo(ParkingPreemption.Result.PREEMPTED);
        // prev liberada
        verify(spotRepo).save(argThat(p -> p.getId().equals(100L) && p.getOccupiedBySessionId() == null));
        // s agora está em dest
        assertThat(s.getSpot()).isEqualTo(dest);
    }

    @Test
    void preemption_relocate_swap_failed_rolls_back_alt() {
        var sec = sector(1L);
        var dest = spot(200L, 99L, sec);
        var alt = spot(300L, null, sec);
        var s = session(10L);

        when(spotRepo.findFirstFreeInSector(1L)).thenReturn(Optional.of(alt));
        when(spotRepo.tryOccupy(300L, 99L)).thenReturn(1);   // ocupa alt para T
        when(spotRepo.trySwapOccupant(200L, 99L, 10L)).thenReturn(0); // falha no swap

        var r = preemption.placeOrPreempt(s, dest);

        assertThat(r).isEqualTo(ParkingPreemption.Result.DENIED);
        // alt liberada (rollback manual)
        verify(spotRepo).save(argThat(p -> p.getId().equals(300L) && p.getOccupiedBySessionId() == null));
    }

    @Test
    void preemption_double_swap_success_when_alt_is_prev() {
        var sec = sector(1L);
        var prev = spot(100L, 10L, sec);    // X: de S=10
        var dest = spot(200L, 99L, sec);    // Y: de T=99

        var s = session(10L); s.setSpot(prev);

        // sem vaga livre; alt será o prev
        when(spotRepo.findFirstFreeInSector(1L)).thenReturn(Optional.empty());
        // 1º swap: X S->T
        when(spotRepo.trySwapOccupant(100L, 10L, 99L)).thenReturn(1);
        // 2º swap: Y T->S
        when(spotRepo.trySwapOccupant(200L, 99L, 10L)).thenReturn(1);

        var r = preemption.placeOrPreempt(s, dest);

        assertThat(r).isEqualTo(ParkingPreemption.Result.PREEMPTED);
        assertThat(s.getSpot()).isEqualTo(dest);
    }

    @Test
    void preemption_double_swap_second_fail_throws_for_rollback() {
        var sec = sector(1L);
        var prev = spot(100L, 10L, sec);
        var dest = spot(200L, 99L, sec);
        var s = session(10L); s.setSpot(prev);

        when(spotRepo.findFirstFreeInSector(1L)).thenReturn(Optional.empty());
        when(spotRepo.trySwapOccupant(100L, 10L, 99L)).thenReturn(1); // X: ok
        when(spotRepo.trySwapOccupant(200L, 99L, 10L)).thenReturn(0); // Y: falha

        assertThatThrownBy(() -> preemption.placeOrPreempt(s, dest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Swap Y falhou");

        // como é unit (sem @Transactional), não há rollback real aqui; no service haverá.
    }
}

