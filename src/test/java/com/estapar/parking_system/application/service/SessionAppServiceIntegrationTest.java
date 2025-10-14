package com.estapar.parking_system.application.service;

import com.estapar.parking_system.api.dto.WebhookDtos;
import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import com.estapar.parking_system.domain.entity.VehicleSessionEntity;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "app.bootstrap.enabled=false",          // não chama o simulador /bootstrap
        "spring.jpa.hibernate.ddl-auto=none",   // usamos Flyway
        "spring.flyway.enabled=true"
})
class SessionAppServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("estapar")
            .withUsername("root")
            .withPassword("root");

    @DynamicPropertySource
    static void configureProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired private SessionAppService service;
    @Autowired private VehicleSessionRepository sessionRepo;
    @Autowired private SectorRepository sectorRepo;
    @Autowired private SpotRepository spotRepo;

    private SectorEntity sectorA;
    private SpotEntity spotA1;

    @BeforeEach
    void setupData() {
        // cria setor A (basePrice 40.50, capacidade 10)
        sectorA = new SectorEntity();
        sectorA.setCode("A");
        sectorA.setBasePrice(new BigDecimal("40.50"));
        sectorA.setMaxCapacity(10);
        sectorA.setOpenHour("00:00");
        sectorA.setCloseHour("23:59");
        sectorA.setDurationLimitMinutes(1440);
        sectorA = sectorRepo.save(sectorA);

        // cria uma vaga no setor A
        spotA1 = new SpotEntity();
        spotA1.setId(1L);
        spotA1.setSector(sectorA);
        spotA1.setLat(new BigDecimal("-23.561684"));
        spotA1.setLng(new BigDecimal("-46.655981"));
        spotA1.setOccupiedBySessionId(null);
        spotA1 = spotRepo.save(spotA1);
    }

    @AfterEach
    void clean() {
        sessionRepo.deleteAll();
        spotRepo.deleteAll();
        sectorRepo.deleteAll();
    }

    @Test
    void fullFlow_entry_parked_exit_shouldPersistAndCharge() {
        // --- ENTRY ---
        var entryTime = "2025-01-01T12:00:00Z";
        var plate = "ZUL0001";
        service.handleEntry(new WebhookDtos.EntryEvent(
                plate, entryTime, WebhookDtos.EventType.ENTRY));

        var open = sessionRepo.findOpenByPlate(plate).orElse(null);
        assertNotNull(open, "ENTRY deve criar sessão aberta");
        assertEquals(plate, open.getLicensePlate());
        assertNotNull(open.getEntryTime());
        assertNull(open.getExitTime());
        assertNotNull(open.getPriceFactor(), "priceFactor calculado no ENTRY");
        // primeiro carro: ocupação ~0 → fator esperado 0.90 (desconto 10%)
        assertEquals(new BigDecimal("0.90"), open.getPriceFactor());

        // --- PARKED ---
        service.handleParked(new WebhookDtos.ParkedEvent(
                plate,
                new BigDecimal("-23.561684"),
                new BigDecimal("-46.655981"),
                WebhookDtos.EventType.PARKED
        ));

        var parked = sessionRepo.findOpenByPlate(plate).orElse(null);
        assertNotNull(parked);
        assertNotNull(parked.getSpot(), "deve vincular spot no PARKED");
        assertEquals(spotA1.getId(), parked.getSpot().getId());
        assertNotNull(parked.getSector(), "deve setar sector no PARKED");
        assertEquals(sectorA.getId(), parked.getSector().getId());
        assertEquals(new BigDecimal("40.50"), parked.getBasePrice(), "basePrice herdado do setor");
        // vaga fisicamente ocupada
        var refreshedSpot = spotRepo.findById(spotA1.getId()).orElseThrow();
        assertEquals(parked.getId(), refreshedSpot.getOccupiedBySessionId());

        // --- EXIT ---
        // permanência = 1h40 → 100 min → 30 min grátis → 70 min → arredonda para 2h
        // 2h * 40.50 * 0.90 = 72.90
        var exitTime = "2025-01-01T13:40:00Z";
        service.handleExit(new WebhookDtos.ExitEvent(
                plate, exitTime, WebhookDtos.EventType.EXIT));

        VehicleSessionEntity closed = sessionRepo.findOpenByPlate(plate).orElse(null);
        assertNull(closed, "deve encerrar sessão no EXIT");

        // pega a última sessão fechada do plate
        var sessions = sessionRepo.findAll();
        assertEquals(1, sessions.size(), "deve existir 1 sessão no total");
        var s = sessions.getFirst();
        assertNotNull(s.getExitTime());
        assertEquals(new BigDecimal("72.90"), s.getChargedAmount());

        // vaga liberada
        var freedSpot = spotRepo.findById(spotA1.getId()).orElseThrow();
        assertNull(freedSpot.getOccupiedBySessionId(), "vaga deve ser liberada no EXIT");
    }

    @Test
    void parked_without_entry_should_be_ignored() {
        service.handleParked(new WebhookDtos.ParkedEvent(
                "NOENTRY",
                new BigDecimal("-23.561684"),
                new BigDecimal("-46.655981"),
                WebhookDtos.EventType.PARKED
        ));
        assertTrue(sessionRepo.findAll().isEmpty(), "PARKED sem ENTRY deve ser ignorado");
    }

    @Test
    void exit_without_entry_should_be_ignored() {
        service.handleExit(new WebhookDtos.ExitEvent(
                "NOENTRY", "2025-01-01T12:10:00Z", WebhookDtos.EventType.EXIT
        ));
        assertTrue(sessionRepo.findAll().isEmpty(), "EXIT sem ENTRY deve ser ignorado");
    }
}

