package com.estapar.parking_system.application.helpers;


import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import com.estapar.parking_system.domain.entity.VehicleSessionEntity;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static com.estapar.parking_system.application.helpers.ParkingPreemption.Result.*;
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

    private static SectorEntity sector(long id, String code, String base) {
        var s = new SectorEntity();
        s.setId(id);
        s.setCode(code);
        s.setBasePrice(new BigDecimal(base));
        return s;
    }

    @Test
    void free_destination_tryOccupy_success_places_and_releases_prev() {
        var sec = sector(1L, "A", "40.50");
        var dest = spot(200L, null, sec);
        var prev = spot(100L, 10L, sec);

        var s = session(10L);
        s.setSpot(prev);
        s.setSector(sec);
        s.setBasePrice(sec.getBasePrice());

        when(spotRepo.tryOccupy(200L, 10L)).thenReturn(1);

        var r = preemption.placeOrPreempt(s, dest);

        assertThat(r).isEqualTo(PLACED_FREE);
        // prev foi liberada
        verify(spotRepo).save(argThat(p -> p.getId().equals(100L) && p.getOccupiedBySessionId() == null));
        // s agora aponta para dest e mantém setor/basePrice iguais (mesmo setor)
        assertThat(s.getSpot()).isEqualTo(dest);
        assertThat(s.getSector()).isEqualTo(sec);
        assertThat(s.getBasePrice()).isEqualByComparingTo(new BigDecimal("40.50"));
    }

    @Test
    void free_destination_tryOccupy_race_lost_noop() {
        var sec = sector(1L, "A", "40.50");
        var dest = spot(200L, null, sec);
        var s = session(10L);

        when(spotRepo.tryOccupy(200L, 10L)).thenReturn(0);

        var r = preemption.placeOrPreempt(s, dest);
        assertThat(r).isEqualTo(NOOP);
        assertThat(s.getSpot()).isNull();
        assertThat(s.getSector()).isNull();
        assertThat(s.getBasePrice()).isNull();
    }

    @Test
    void preemption_denied_no_alternative() {
        var sec = sector(1L, "A", "40.50");
        var dest = spot(200L, 99L, sec); // ocupado por T=99
        var s = session(10L); // S=10

        when(spotRepo.findFirstBySector_IdAndOccupiedBySessionIdIsNullOrderByIdAsc(1L))
                .thenReturn(Optional.empty());
        when(sectorRepo.findAll()).thenReturn(java.util.List.of(sec));

        var r = preemption.placeOrPreempt(s, dest);

        assertThat(r).isEqualTo(DENIED);

        verify(spotRepo, atLeastOnce()).findFirstBySector_IdAndOccupiedBySessionIdIsNullOrderByIdAsc(1L);        verify(spotRepo, never()).tryOccupy(anyLong(), anyLong());
        verify(spotRepo, never()).trySwapOccupant(anyLong(), anyLong(), anyLong());
        verify(spotRepo, never()).save(any());
    }

    /*@Test
    void preemption_relocate_then_swap_success_updates_base_price_if_sector_changes() {
        var secA = sector(1L, "A", "40.50");
        var secB = sector(2L, "B", "4.10");

        var dest = spot(200L, 99L, secB); // Y ocupado por T=99 (setor B)
        var alt = spot(300L, null, secB); // vaga livre p/ T (mesmo setor do destino)
        var prev = spot(100L, 10L, secA); // X atual de S=10 (setor A)

        var s = session(10L);
        s.setSpot(prev);
        s.setSector(secA);
        s.setBasePrice(secA.getBasePrice()); // 40.50 → deve virar 4.10 após mover p/ setor B

        when(spotRepo.findFirstFreeInSector(2L)).thenReturn(Optional.of(alt));
        when(spotRepo.tryOccupy(300L, 99L)).thenReturn(1);          // realoca T em alt
        when(spotRepo.trySwapOccupant(200L, 99L, 10L)).thenReturn(1); // swap Y: T->S

        var r = preemption.placeOrPreempt(s, dest);

        assertThat(r).isEqualTo(PREEMPTED);
        // prev liberada
        verify(spotRepo).save(argThat(p -> p.getId().equals(100L) && p.getOccupiedBySessionId() == null));
        // s agora está em dest (setor B) e basePrice = 4.10
        assertThat(s.getSpot()).isEqualTo(dest);
        assertThat(s.getSector()).isEqualTo(secB);
        assertThat(s.getBasePrice()).isEqualByComparingTo(new BigDecimal("4.10"));
    }



    @Test
    void preemption_relocate_swap_failed_rolls_back_alt() {
        var sec = sector(1L, "A", "40.50");
        var dest = spot(200L, 99L, sec);
        var alt = spot(300L, null, sec);
        var s = session(10L);

        when(spotRepo.findFirstFreeInSector(1L)).thenReturn(Optional.of(alt));
        when(spotRepo.tryOccupy(300L, 99L)).thenReturn(1);   // ocupa alt para T
        when(spotRepo.trySwapOccupant(200L, 99L, 10L)).thenReturn(0); // falha no swap

        var r = preemption.placeOrPreempt(s, dest);

        assertThat(r).isEqualTo(DENIED);
        // alt liberada (rollback manual)
        verify(spotRepo).save(argThat(p -> p.getId().equals(300L) && p.getOccupiedBySessionId() == null));
    }
*/
    @Test
    void preemption_double_swap_success_when_alt_is_prev_updates_base_price_if_sector_changes() {
        var secA = sector(1L, "A", "40.50");
        var secB = sector(2L, "B", "4.10");

        var prev = spot(100L, 10L, secA);   // X: de S=10 (setor A)
        var dest = spot(200L, 99L, secB);   // Y: de T=99 (setor B)

        var s = session(10L);
        s.setSpot(prev);
        s.setSector(secA);
        s.setBasePrice(secA.getBasePrice());

        // sem vaga livre no setor B; alt será o prev
        when(spotRepo.findFirstFreeInSector(2L)).thenReturn(Optional.empty());
        // 1º swap: X S->T
        when(spotRepo.trySwapOccupant(100L, 10L, 99L)).thenReturn(1);
        // 2º swap: Y T->S
        when(spotRepo.trySwapOccupant(200L, 99L, 10L)).thenReturn(1);

        var r = preemption.placeOrPreempt(s, dest);

        assertThat(r).isEqualTo(PREEMPTED);
        assertThat(s.getSpot()).isEqualTo(dest);
        assertThat(s.getSector()).isEqualTo(secB);
        assertThat(s.getBasePrice()).isEqualByComparingTo(new BigDecimal("4.10"));
    }

    /*@Test
    void preemption_double_swap_second_fail_throws_for_rollback() {
        var sec = sector(1L, "A", "40.50");
        var prev = spot(100L, 10L, sec);
        var dest = spot(200L, 99L, sec);
        var s = session(10L);
        s.setSpot(prev);

        when(spotRepo.findFirstBySector_IdAndOccupiedBySessionIdIsNullOrderByIdAsc(1L))
                .thenReturn(Optional.empty());
        when(spotRepo.trySwapOccupant(100L, 10L, 99L)).thenReturn(1); // X: ok
        when(spotRepo.trySwapOccupant(200L, 99L, 10L)).thenReturn(0); // Y: falha

        assertThatThrownBy(() -> preemption.placeOrPreempt(s, dest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Swap Y falhou");

    }

     */
}

