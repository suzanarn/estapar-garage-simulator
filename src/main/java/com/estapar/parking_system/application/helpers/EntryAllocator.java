package com.estapar.parking_system.application.helpers;

import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.service.OccupancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntryAllocator {

    private final SectorRepository sectorRepo;
    private final SpotRepository spotRepo;
    private final OccupancyService occupancy;

    public record Allocation(SectorEntity sector, SpotEntity spot) {}

    /** Try to reserve (atomically) a spot in any available sector */
    public Allocation allocateForSession(Long sessionId) {
        for (SectorEntity sector : sectorRepo.findAll()) {
            if (occupancy.isSectorFull(sector.getId())) continue;
            while (true) {
                var free = spotRepo.findFirstBySector_IdAndOccupiedBySessionIdIsNullOrderByIdAsc(sector.getId());
                if (free.isEmpty()) break;

                var candidate = free.get();
                if (spotRepo.tryOccupy(candidate.getId(), sessionId) == 1) {
                    return new Allocation(sector, candidate);
                }
            }
        }
        return null;
    }
}