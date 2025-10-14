package com.estapar.parking_system.config;

import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import com.estapar.parking_system.domain.service.OccupancyService;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.service.DynamicFactorService;
import com.estapar.parking_system.domain.service.PricingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainBeansConfig {

    @Bean
    public OccupancyService occupancyService(SpotRepository spotRepo,
                                             VehicleSessionRepository sessionRepo) {
        return new OccupancyService(spotRepo, sessionRepo);
    }
    @Bean
    public DynamicFactorService dynamicFactorService() {
        return new DynamicFactorService();
    }

    @Bean
    public PricingService pricingService() {
        return new PricingService();
    }
}
