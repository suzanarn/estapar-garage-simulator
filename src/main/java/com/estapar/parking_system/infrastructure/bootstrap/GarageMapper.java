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
        dst.setBasePrice(src.base_price());
        dst.setMaxCapacity(src.max_capacity());
        dst.setOpenHour(src.open_hour());
        dst.setCloseHour(src.close_hour());
        dst.setDurationLimitMinutes(src.duration_limit_minutes());
    }

    public void applyToEntity(SpotDto src, SpotEntity dst) {
        dst.setId(src.id());
        dst.setLat(src.lat());
        dst.setLng(src.lng());
    }
}

