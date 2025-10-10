package com.estapar.parking_system.infrastructure.bootstrap;

import com.estapar.parking_system.api.dto.GarageDtos.SpotDto;
import com.estapar.parking_system.api.dto.GarageDtos.SectorDto;
import com.estapar.parking_system.api.dto.GarageDtos.GarageResponse;
import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import com.estapar.parking_system.domain.repository.SectorRepository;
import com.estapar.parking_system.domain.repository.SpotRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class GarageSynchronizer {
    private static final Logger log = LoggerFactory.getLogger(GarageSynchronizer.class);

    private final SectorRepository sectorRepo;
    private final SpotRepository spotRepo;
    private final GarageMapper mapper;

    @Transactional
    public void sync(GarageResponse response){
        response.garage().forEach(this::upsertSector);
        response.spots().forEach(this::upsertSpot);
        log.info("Garage sync completed: sectors={}, spots={}", sectorRepo.count(), spotRepo.count());
    }

    private void upsertSector(SectorDto dto){
        //TODO: creat a 404 exception and use exception handler
        SectorEntity entity = sectorRepo.findByCode(dto.sector()).orElseGet(SectorEntity::new);
        mapper.applyToEntity(dto, entity);

        sectorRepo.save(entity);
    }

    private void upsertSpot(SpotDto dto){
        //TODO: creat a 404 exception and use exception handler
        SectorEntity sector = sectorRepo.findByCode(dto.sector())
                .orElseThrow(() -> new IllegalStateException("Sector not found: " + dto.sector()));

        SpotEntity spot = spotRepo.findById(dto.id())
                .orElseGet(SpotEntity::new);

        mapper.applyToEntity(dto, spot);
        spot.setSector(sector);

        spotRepo.save(spot);
    }
}
