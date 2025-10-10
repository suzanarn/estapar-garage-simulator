package com.estapar.parking_system.domain.repository;

import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpotRepository extends JpaRepository<SpotEntity, Long> {
    List<SpotEntity> findBySector(SectorEntity sector);
}
