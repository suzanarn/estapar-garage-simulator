package com.estapar.parking_system.application.service;

import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    @Mock
    private VehicleSessionRepository sessionRepo;

    private RevenueService service;

    @BeforeEach
    void setUp() {
        service = new RevenueService(sessionRepo);
    }

    @Test
    void shouldComputeRevenueForDateAndSector_inUTCWindow_andScale2() {
        String date = "2025-01-01";
        String sector = "A";

        Instant expectedStart = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant expectedEnd   = LocalDate.parse(date).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        when(sessionRepo.sumChargedBetweenAndSector(any(), any(), any()))
                .thenReturn(new BigDecimal("123.45"));

        var resp = service.revenueForDate(date, sector);

        ArgumentCaptor<Instant> startCap = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> endCap   = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<String> sectorCap = ArgumentCaptor.forClass(String.class);

        verify(sessionRepo).sumChargedBetweenAndSector(startCap.capture(), endCap.capture(), sectorCap.capture());

        assertThat(startCap.getValue()).isEqualTo(expectedStart);
        assertThat(endCap.getValue()).isEqualTo(expectedEnd);
        assertThat(sectorCap.getValue()).isEqualTo("A");

        assertThat(resp.amount()).isEqualByComparingTo(new BigDecimal("123.45"));
        assertThat(resp.currency()).isEqualTo("BRL");
        assertThat(resp.timestamp()).isNotBlank();
    }

    @Test
    void shouldReturnZeroWhenRepositoryReturnsNull() {
        when(sessionRepo.sumChargedBetweenAndSector(any(), any(), any()))
                .thenReturn(null);

        var resp = service.revenueForDate("2025-01-01", "A");

        assertThat(resp.amount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(resp.currency()).isEqualTo("BRL");
    }

    @Test
    void shouldTreatBlankSectorAsNull() {
        when(sessionRepo.sumChargedBetweenAndSector(any(), any(), isNull()))
                .thenReturn(new BigDecimal("10.00"));

        var resp = service.revenueForDate("2025-01-01", "   ");

        assertThat(resp.amount()).isEqualByComparingTo(new BigDecimal("10.00"));
        verify(sessionRepo).sumChargedBetweenAndSector(any(), any(), isNull());
    }
}
