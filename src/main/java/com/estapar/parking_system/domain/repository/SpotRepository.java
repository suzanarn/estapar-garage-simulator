package com.estapar.parking_system.domain.repository;

import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SpotRepository extends JpaRepository<SpotEntity, Long> {
    List<SpotEntity> findBySector(SectorEntity sector);

    @Query("SELECT COUNT(s) FROM SpotEntity s WHERE s.occupiedBySessionId IS NOT NULL")
    long countOccupiedSpots();

    Optional<SpotEntity> findByLatAndLng(BigDecimal lat, BigDecimal lng);

}
