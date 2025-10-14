package com.estapar.parking_system.infrastructure.bootstrap;


import com.estapar.parking_system.api.dto.GarageDtos.SectorDto;
import com.estapar.parking_system.api.dto.GarageDtos.SpotDto;
import com.estapar.parking_system.domain.entity.SectorEntity;
import com.estapar.parking_system.domain.entity.SpotEntity;
import org.springframework.stereotype.Component;

@Component
public class GarageMapper {

    public void applyToEntity(SectorDto src, SectorEntity dst) {
        dst.setCode(src.sector());
        dst.setBasePrice(src.basePrice());
        dst.setMaxCapacity(src.maxCapacity());
        dst.setOpenHour(src.openHour());
        dst.setCloseHour(src.closeHour());
        dst.setDurationLimitMinutes(src.durationLimitMinutes());
    }

    public void applyToEntity(SpotDto src, SpotEntity dst) {
        dst.setId(src.id());
        dst.setLat(src.lat());
        dst.setLng(src.lng());
    }
}

