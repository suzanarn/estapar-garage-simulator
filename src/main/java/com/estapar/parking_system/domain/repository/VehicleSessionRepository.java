package com.estapar.parking_system.domain.repository;

import com.estapar.parking_system.domain.entity.VehicleSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface VehicleSessionRepository extends JpaRepository<VehicleSessionEntity, Long> {
    @Query("select vs from VehicleSessionEntity vs " +
            "where vs.licensePlate = :plate and vs.exitTime is null")
    Optional<VehicleSessionEntity> findOpenByPlate(String plate);

    @Query("""
       select coalesce(sum(vs.chargedAmount), 0)
       from VehicleSessionEntity vs
       where vs.exitTime >= :start and vs.exitTime < :end
         and (:sectorCode is null or vs.sector.code = :sectorCode)
       """)
    BigDecimal sumChargedBetweenAndSector(Instant start, Instant end, String sectorCode);

    // pega a mais recente entre as abertas (setMaxResults(1))
    Optional<VehicleSessionEntity> findTopByLicensePlateAndExitTimeIsNullOrderByIdDesc(String plate);

    @Query("select count(vs) from VehicleSessionEntity vs " +
            "where vs.licensePlate = :plate and vs.exitTime is null")
    long countOpenByPlate(String plate);

}
