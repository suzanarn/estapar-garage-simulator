package com.estapar.parking_system.domain.service;
import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OccupancyServiceTest {

    SpotRepository spotRepo;
    SectorRepository sectorRepo;
    OccupancyService service;

    @BeforeEach
    void setUp() {
        spotRepo = mock(SpotRepository.class);
        sectorRepo = mock(SectorRepository.class);
        service = new OccupancyService(spotRepo, sectorRepo);
    }

    @Test
    void totalCapacity_returns_spot_count() {
        when(spotRepo.count()).thenReturn(150L);
        assertThat(service.totalCapacity()).isEqualTo(150L);
        verify(spotRepo).count();
    }

    @Test
    void takenSpotsGlobal_returns_occupied_count() {
        when(spotRepo.countOccupiedGlobal()).thenReturn(37L);
        assertThat(service.takenSpotsGlobal()).isEqualTo(37L);
        verify(spotRepo).countOccupiedGlobal();
    }

    @Test
    void globalRatioBySpots_zero_total_returns_zero() {
        when(spotRepo.count()).thenReturn(0L);
        // mesmo que occupied > 0, com total=0 devolve 0 de forma segura
        when(spotRepo.countOccupiedGlobal()).thenReturn(25L);

        assertThat(service.globalRatioBySpots()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void globalRatioBySpots_regular_rounds_half_up_to_two_decimals() {
        // taken = 37, total = 150 -> 37/150 = 0.246666... -> 0.25 (duas casas, HALF_UP)
        when(spotRepo.count()).thenReturn(150L);
        when(spotRepo.countOccupiedGlobal()).thenReturn(37L);

        assertThat(service.globalRatioBySpots()).isEqualByComparingTo("0.25");
    }

    @Test
    void isSectorFull_returns_false_when_taken_lt_max() {
        Long sectorId = 10L;
        when(spotRepo.countOccupiedInSector(sectorId)).thenReturn(49L);
        var sector = new SectorEntity(); sector.setId(sectorId); sector.setMaxCapacity(50);
        when(sectorRepo.findById(sectorId)).thenReturn(Optional.of(sector));

        assertThat(service.isSectorFull(sectorId)).isFalse();
        verify(spotRepo).countOccupiedInSector(sectorId);
        verify(sectorRepo).findById(sectorId);
    }

    @Test
    void isSectorFull_returns_true_when_taken_eq_max() {
        Long sectorId = 10L;
        when(spotRepo.countOccupiedInSector(sectorId)).thenReturn(50L);
        var sector = new SectorEntity(); sector.setId(sectorId); sector.setMaxCapacity(50);
        when(sectorRepo.findById(sectorId)).thenReturn(Optional.of(sector));

        assertThat(service.isSectorFull(sectorId)).isTrue();
    }

    @Test
    void isSectorFull_returns_true_when_taken_gt_max() {
        Long sectorId = 10L;
        when(spotRepo.countOccupiedInSector(sectorId)).thenReturn(51L);
        var sector = new SectorEntity(); sector.setId(sectorId); sector.setMaxCapacity(50);
        when(sectorRepo.findById(sectorId)).thenReturn(Optional.of(sector));

        assertThat(service.isSectorFull(sectorId)).isTrue();
    }

    @Test
    void isSectorFull_when_sector_not_found_treats_max_as_zero() {
        Long sectorId = 99L;
        when(spotRepo.countOccupiedInSector(sectorId)).thenReturn(0L);
        when(sectorRepo.findById(sectorId)).thenReturn(Optional.empty());

        // max = 0 (default) -> taken(0) >= max(0) => true
        assertThat(service.isSectorFull(sectorId)).isTrue();
    }
}
/*

import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import com.estapar.parking_system.domain.service.OccupancyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class OccupancyServiceTest {

    private SpotRepository spotRepo;
    private VehicleSessionRepository sessionRepo;
    private OccupancyService service;
    private SectorRepository sectorRepo;

    @BeforeEach
    void setup() {
        spotRepo = Mockito.mock(SpotRepository.class);
        sectorRepo = Mockito.mock(SectorRepository.class);
        service = new OccupancyService(spotRepo, sectorRepo);
    }

    @Test
    void shouldReturnZeroWhenNoSpotsExist() {
        when(spotRepo.count()).thenReturn(0L);
        assertEquals(BigDecimal.ZERO, service.globalRatioBySessions());
    }

    @Test
    void shouldComputeGlobalRatioCorrectly() {
        when(spotRepo.count()).thenReturn(100L);
        when(sessionRepo.countOpenSessions()).thenReturn(40L);

        BigDecimal ratio = service.globalRatioBySessions();
        assertEquals(new BigDecimal("0.40"), ratio);
    }

    @Test
    void shouldDetectGarageFullWhenOpenSessionsEqualTotal() {
        when(spotRepo.count()).thenReturn(10L);
        when(sessionRepo.countOpenSessions()).thenReturn(10L);

        assertTrue(service.isGarageFullBySessions());
    }

    @Test
    void shouldDetectGarageNotFullWhenOpenSessionsLessThanTotal() {
        when(spotRepo.count()).thenReturn(10L);
        when(sessionRepo.countOpenSessions()).thenReturn(5L);

        assertFalse(service.isGarageFullBySessions());
    }
}
*/
