package com.estapar.parking_system.application.helpers;

import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import com.estapar.parking_system.domain.entity.VehicleSessionEntity;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ParkingPreemption {

    private final SpotRepository spotRepo;
    private final SectorRepository sectorRepo;
    private final VehicleSessionRepository sessionRepo;

    public enum Result { NOOP, PLACED_FREE, PREEMPTED, DENIED }

    @Transactional
    public Result placeOrPreempt(VehicleSessionEntity session, SpotEntity destination) {
        // idempotent
        if (Objects.equals(session.getId(), destination.getOccupiedBySessionId())) return Result.NOOP;

        if (destination.getOccupiedBySessionId() == null) {
            return tryPlaceOnFreeSpot(session, destination);
        }

        //destination is occupied
        Long displacedId = destination.getOccupiedBySessionId();
        SpotEntity prevSpot = session.getSpot();

        // try direct swap (when the alternative is exactly the previous spot of the session itself)
        Optional<SpotEntity> alt = findAlternativeForDisplaced(prevSpot, destination.getSector().getId());
        if (alt.isEmpty()) return Result.DENIED;

        SpotEntity alternative = alt.get();

        if (prevSpot != null && Objects.equals(alternative.getId(), prevSpot.getId())) {
            return tryDirectSwap(session, prevSpot, destination, displacedId);
        }

        // relocate the displaced person to the alternative and then occupy the destination
        return tryRelocateAndSwap(session, destination, displacedId, prevSpot, alternative);
    }


    private Result tryPlaceOnFreeSpot(VehicleSessionEntity session, SpotEntity destination) {
        int updated = spotRepo.tryOccupy(destination.getId(), session.getId());
        if (updated == 0) return Result.NOOP;

        releasePreviousIfOwned(session);
        attachSessionToSpot(session, destination);
        return Result.PLACED_FREE;
    }

    /** manage if my reserved spot is the one someone wants to park **/
    private Result tryDirectSwap(VehicleSessionEntity session,
                                 SpotEntity previousSpot,
                                 SpotEntity destination,
                                 Long displacedId) {

        int movedX = spotRepo.trySwapOccupant(previousSpot.getId(), session.getId(), displacedId);
        if (movedX == 0) return Result.DENIED;

        int swappedY = spotRepo.trySwapOccupant(destination.getId(), displacedId, session.getId());
        if (swappedY == 0) {
            spotRepo.trySwapOccupant(previousSpot.getId(), displacedId, session.getId());
            return Result.DENIED;
        }

        attachSessionToSpot(session, destination);
        updateDisplacedSessionTo(previousSpot, displacedId);
        return Result.PREEMPTED;
    }

    /** try to relocate **/
    private Result tryRelocateAndSwap(VehicleSessionEntity session,
                                      SpotEntity destination,
                                      Long displacedId,
                                      SpotEntity previousSpot,
                                      SpotEntity alternativeSpot) {

        int moved = spotRepo.tryOccupy(alternativeSpot.getId(), displacedId);
        if (moved == 0) return Result.DENIED;

        int swapped = spotRepo.trySwapOccupant(destination.getId(), displacedId, session.getId());
        if (swapped == 0) {
            alternativeSpot.setOccupiedBySessionId(null);
            spotRepo.save(alternativeSpot);
            return Result.DENIED;
        }

        if (previousSpot != null && Objects.equals(session.getId(), previousSpot.getOccupiedBySessionId())) {
            previousSpot.setOccupiedBySessionId(null);
            spotRepo.save(previousSpot);
        }

        attachSessionToSpot(session, destination);
        updateDisplacedSessionTo(alternativeSpot, displacedId);
        return Result.PREEMPTED;
    }

/** get free spot in the same sector or in another **/
    private Optional<SpotEntity> findAlternativeForDisplaced(SpotEntity previousOfRequester, Long destSectorId) {
        Optional<SpotEntity> sameSector = spotRepo.findFirstBySector_IdAndOccupiedBySessionIdIsNullOrderByIdAsc(destSectorId);
        if (sameSector.isPresent()) return sameSector;
        if (previousOfRequester != null) return Optional.of(previousOfRequester);

        for (SectorEntity sector : sectorRepo.findAll()) {
            if (Objects.equals(sector.getId(), destSectorId)) continue;
            Optional<SpotEntity> other = spotRepo
                    .findFirstBySector_IdAndOccupiedBySessionIdIsNullOrderByIdAsc(sector.getId());
            if (other.isPresent()) return other;
        }
        return Optional.empty();
    }

    private void releasePreviousIfOwned(VehicleSessionEntity session) {
        SpotEntity prev = session.getSpot();
        if (prev != null && Objects.equals(session.getId(), prev.getOccupiedBySessionId())) {
            prev.setOccupiedBySessionId(null);
            spotRepo.save(prev);
        }
    }

    /** attach sector  **/
    public static void attachSessionToSpot(VehicleSessionEntity session, SpotEntity spot) {
        SectorEntity sector = spot.getSector();
        if (session.getSector() == null || !Objects.equals(session.getSector().getId(), sector.getId())) {
            session.setSector(sector);
            session.setBasePrice(sector.getBasePrice());
        }

        if (session.getBasePrice() == null) {
            session.setBasePrice(sector.getBasePrice());
        }
        session.setSpot(spot);
    }

    /** update displaced spot  **/
    private void updateDisplacedSessionTo(SpotEntity newSpot, Long displacedSessionId) {
        sessionRepo.findById(displacedSessionId).ifPresent(displaced -> {
            displaced.setSpot(newSpot);
            SectorEntity sector = newSpot.getSector();
            if (displaced.getSector() == null || !Objects.equals(displaced.getSector().getId(), sector.getId())) {
                displaced.setSector(sector);
                displaced.setBasePrice(sector.getBasePrice());
            } else if (displaced.getBasePrice() == null) {
                displaced.setBasePrice(sector.getBasePrice());
            }
            sessionRepo.save(displaced);
        });
    }
}
