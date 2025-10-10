package com.estapar.parking_system.domain.repository;

import com.estapar.parking_system.domain.entity.SectorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SectorRepository extends JpaRepository<SectorEntity, Long> {
    Optional<SectorEntity> findByCode(String code);
}
