package com.estapar.parking_system.domain.service;

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

    @BeforeEach
    void setup() {
        spotRepo = Mockito.mock(SpotRepository.class);
        sessionRepo = Mockito.mock(VehicleSessionRepository.class);
        service = new OccupancyService(spotRepo, sessionRepo);
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

