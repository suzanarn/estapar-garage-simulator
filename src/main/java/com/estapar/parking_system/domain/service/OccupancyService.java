package com.estapar.parking_system.domain.service;

import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@AllArgsConstructor
public class OccupancyService {
        private final SpotRepository spotRepository;
        private final SectorRepository sectorRepository;


        /** Capacidade total (todas as vagas físicas) */
        public long totalCapacity() {
            return spotRepository.count();
        }

        /** Vagas já tomadas (ocupadas/reservadas) — usamos occupied como “reserva lógica” */
        public long takenSpotsGlobal() {
            return spotRepository.countOccupiedGlobal();
        }

        /** Ratio global por VAGAS, para preço dinâmico na ENTRADA */
        public BigDecimal globalRatioBySpots() {
            long total = totalCapacity();
            if (total == 0) return BigDecimal.ZERO;
            return BigDecimal.valueOf(takenSpotsGlobal())
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }

        /** Setor cheio quando vagas tomadas no setor >= maxCapacity do setor */
        public boolean isSectorFull(Long sectorId) {
            long taken = spotRepository.countOccupiedInSector(sectorId);
            int max = sectorRepository.findById(sectorId)
                    .map(SectorEntity::getMaxCapacity).orElse(0);
            return taken >= max;
        }
}
