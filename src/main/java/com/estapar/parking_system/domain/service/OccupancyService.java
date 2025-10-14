package com.estapar.parking_system.domain.service;

import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class OccupancyService {
    private final SpotRepository spotRepository;
    private final VehicleSessionRepository sessionRepository;

    public OccupancyService(SpotRepository spotRepository,
                            VehicleSessionRepository sessionRepository) {
        this.spotRepository = spotRepository;
        this.sessionRepository = sessionRepository;
    }

    /** Capacidade total da garagem (total de vagas físicas) */
    public long totalCapacity() {
        return spotRepository.count();
    }

    /** Quantas “unidades de capacidade” já estão consumidas por sessões abertas */
    public long openSessions() {
        return sessionRepository.countOpenSessions();
    }

    /** Ratio global baseado em sessões (reserva no ENTRY) */
    public BigDecimal globalRatioBySessions() {
        long total = totalCapacity();
        if (total == 0) return BigDecimal.ZERO;
        long open = openSessions();
        return BigDecimal.valueOf(open)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    /** true se a garagem está cheia (por sessões abertas) */
    public boolean isGarageFullBySessions() {
        return openSessions() >= totalCapacity();
    }
}
