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


        /** Total capacity (all physical spaces) */
        public long totalCapacity() {
            return spotRepository.count();
        }

        /** Places already taken (occupied/reserved), we use occupied as a “logical reservation” */
        public long takenSpotsGlobal() {
            return spotRepository.countOccupiedGlobal();
        }

        /** Overall ratio per SPOTS, for dynamic pricing at ENTRY */
        public BigDecimal globalRatioBySpots() {
            long total = totalCapacity();
            if (total == 0) return BigDecimal.ZERO;
            return BigDecimal.valueOf(takenSpotsGlobal())
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }

        /** Sector full when vacancies taken in the sector >= maxCapacity of the sector */
        public boolean isSectorFull(Long sectorId) {
            long taken = spotRepository.countOccupiedInSector(sectorId);
            int max = sectorRepository.findById(sectorId)
                    .map(SectorEntity::getMaxCapacity).orElse(0);
            return taken >= max;
        }
}
