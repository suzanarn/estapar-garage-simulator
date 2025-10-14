package com.estapar.parking_system.application.helpers;

import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import com.estapar.parking_system.domain.entity.VehicleSessionEntity;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParkingPreemption {

    private final SpotRepository spotRepo;
    private final SectorRepository sectorRepo;
    private final VehicleSessionRepository sessionRepo;

    public enum Result { NOOP, PLACED_FREE, PREEMPTED, DENIED }

    public Result placeOrPreempt(VehicleSessionEntity session, SpotEntity dest) {
        // Idempotência
        if (session.getId().equals(dest.getOccupiedBySessionId())) return Result.NOOP;

        // Destino livre → tenta ocupar atomically
        if (dest.getOccupiedBySessionId() == null) {
            if (spotRepo.tryOccupy(dest.getId(), session.getId()) == 1) {
                releasePreviousIfOwned(session);
                attachSessionToSpot(session, dest);
                return Result.PLACED_FREE;
            }
            return Result.NOOP;
        }

        // Destino ocupado por T ≠ S → preempção condicional
        Long tSessionId = dest.getOccupiedBySessionId();
        var prev = session.getSpot(); // X (pode ser null)

        var alt = findAlternativeForDisplaced(prev, dest.getSector().getId());
        if (alt == null) return Result.DENIED;

        // Caso especial: alt == X (swap duplo)
        if (prev != null && alt.getId().equals(prev.getId())) {
            int movedX = spotRepo.trySwapOccupant(prev.getId(), session.getId(), tSessionId); // X: S->T
            if (movedX == 0) return Result.DENIED;

            int swappedY = spotRepo.trySwapOccupant(dest.getId(), tSessionId, session.getId()); // Y: T->S
            if (swappedY == 0) throw new IllegalStateException("Swap Y falhou; rollback reverte swap X");

            attachSessionToSpot(session, dest);
            updateDisplacedSessionTo(prev, tSessionId);
            return Result.PREEMPTED;
        }

        // Geral: realoca T para alt (tryOccupy), depois swap Y
        int moved = spotRepo.tryOccupy(alt.getId(), tSessionId);
        if (moved == 0) return Result.DENIED;

        int swapped = spotRepo.trySwapOccupant(dest.getId(), tSessionId, session.getId());
        if (swapped == 1) {
            if (prev != null && session.getId().equals(prev.getOccupiedBySessionId())) {
                prev.setOccupiedBySessionId(null);
                spotRepo.save(prev);
            }
            attachSessionToSpot(session, dest);
            return Result.PREEMPTED;
        } else {
            // desfaz a realocação de T
            alt.setOccupiedBySessionId(null);
            spotRepo.save(alt);
            return Result.DENIED;
        }
    }

    private SpotEntity findAlternativeForDisplaced(SpotEntity prev, Long destSectorId) {
        var same = spotRepo.findFirstFreeInSector(destSectorId);
        if (same.isPresent()) return same.get();
        if (prev != null) return prev;

        for (SectorEntity s : sectorRepo.findAll()) {
            if (s.getId().equals(destSectorId)) continue;
            var other = spotRepo.findFirstFreeInSector(s.getId());
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
        var sector = spot.getSector();
        if (session.getSector() == null || !session.getSector().getId().equals(sector.getId())) {
            session.setSector(sector);
        }
        if (session.getBasePrice() == null) session.setBasePrice(sector.getBasePrice());
        session.setSpot(spot);
    }

    public void updateDisplacedSessionTo(SpotEntity newSpot, Long displacedSessionId) {
        sessionRepo.findById(displacedSessionId).ifPresent(s -> {
            s.setSpot(newSpot);
            SectorEntity sector = newSpot.getSector();
            if (s.getSector() == null || !s.getSector().getId().equals(sector.getId())) {
                s.setSector(sector);
                if (s.getBasePrice() == null) s.setBasePrice(sector.getBasePrice());
            }
            sessionRepo.save(s);
        });
    }
}
