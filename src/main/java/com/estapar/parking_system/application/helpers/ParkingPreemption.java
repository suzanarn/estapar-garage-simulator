package com.estapar.parking_system.application.helpers;

import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import com.estapar.parking_system.domain.entity.VehicleSessionEntity;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ParkingPreemption {

    private final SpotRepository spotRepo;
    private final SectorRepository sectorRepo;
    private final VehicleSessionRepository sessionRepo;

    public enum Result { NOOP, PLACED_FREE, PREEMPTED, DENIED }

    public Result placeOrPreempt(VehicleSessionEntity session, SpotEntity destination) {
        // Idempotence
        if (session.getId().equals(destination.getOccupiedBySessionId())) return Result.NOOP;

        // if estination is free, try to occupy atomically
        if (destination.getOccupiedBySessionId() == null) {
            if (spotRepo.tryOccupy(destination.getId(), session.getId()) == 1) {
                releasePreviousIfOwned(session);
                attachSessionToSpot(session, destination);
                return Result.PLACED_FREE;
            }
            return Result.NOOP;
        }

        Long tempSessionId = destination.getOccupiedBySessionId();
        SpotEntity previousSpot = session.getSpot();

        SpotEntity alternativeSpot = findAlternativeForDisplaced(previousSpot, destination.getSector().getId());
        if (alternativeSpot == null) return Result.DENIED;

        // if alternativeSpot == someone's park spot
        if (previousSpot != null && alternativeSpot.getId().equals(previousSpot.getId())) {
            int movedX = spotRepo.trySwapOccupant(previousSpot.getId(), session.getId(), tempSessionId);
            if (movedX == 0) return Result.DENIED;

            int swappedY = spotRepo.trySwapOccupant(destination.getId(), tempSessionId, session.getId());
            if (swappedY == 0) throw new IllegalStateException("Swap Y falhou; rollback reverte swap X");

            attachSessionToSpot(session, destination);
            updateDisplacedSessionTo(previousSpot, tempSessionId);
            return Result.PREEMPTED;
        }

        // relocate temp to alternativeSpot (tryOccupy), after swap Y
        int moved = spotRepo.tryOccupy(alternativeSpot.getId(), tempSessionId);
        if (moved == 0) return Result.DENIED;

        int swapped = spotRepo.trySwapOccupant(destination.getId(), tempSessionId, session.getId());
        if (swapped == 1) {
            if (previousSpot != null && session.getId().equals(previousSpot.getOccupiedBySessionId())) {
                previousSpot.setOccupiedBySessionId(null);
                spotRepo.save(previousSpot);
            }
            attachSessionToSpot(session, destination);
            return Result.PREEMPTED;
        } else {
            // undo relocation of tempporary
            alternativeSpot.setOccupiedBySessionId(null);
            spotRepo.save(alternativeSpot);
            return Result.DENIED;
        }
    }

    private SpotEntity findAlternativeForDisplaced(SpotEntity prev, Long destSectorId) {
        Optional<SpotEntity> same = spotRepo.findFirstFreeInSector(destSectorId);
        if (same.isPresent()) return same.get();
        if (prev != null) return prev;

        for (SectorEntity sector : sectorRepo.findAll()) {
            if (sector.getId().equals(destSectorId)) continue;
            var other = spotRepo.findFirstFreeInSector(sector.getId());
            if (other.isPresent()) return other.get();
        }
        return null;
    }

    private void releasePreviousIfOwned(VehicleSessionEntity session) {
        var prev = session.getSpot();
        if (prev != null && session.getId().equals(prev.getOccupiedBySessionId())) {
            prev.setOccupiedBySessionId(null);
            spotRepo.save(prev);
        }
    }

    // Estas duas são “sem efeito colateral externo” além da sessão/spot em si:
    public static void attachSessionToSpot(VehicleSessionEntity session, SpotEntity spot) {
        SectorEntity sector = spot.getSector();
        if (session.getSector() == null || !session.getSector().getId().equals(sector.getId())) {
            session.setSector(sector);
        }
        if (session.getBasePrice() == null) session.setBasePrice(sector.getBasePrice());
        session.setSpot(spot);
    }

    public void updateDisplacedSessionTo(SpotEntity newSpot, Long displacedSessionId) {
        sessionRepo.findById(displacedSessionId).ifPresent(session -> {
            session.setSpot(newSpot);
            SectorEntity sector = newSpot.getSector();
            if (session.getSector() == null || !session.getSector().getId().equals(sector.getId())) {
                session.setSector(sector);
                if (session.getBasePrice() == null) session.setBasePrice(sector.getBasePrice());
            }
            sessionRepo.save(session);
        });
    }
}
