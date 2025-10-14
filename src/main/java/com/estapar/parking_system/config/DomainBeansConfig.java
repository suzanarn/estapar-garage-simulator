package com.estapar.parking_system.config;

import com.estapar.parking_system.domain.repository.VehicleSessionRepository;
import com.estapar.parking_system.domain.service.OccupancyService;
import com.estapar.parking_system.domain.repository.SpotRepository;
import com.estapar.parking_system.domain.service.DynamicFactorService;
import com.estapar.parking_system.domain.service.PricingService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
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

    @Bean
    public OpenAPI projectOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Parking System API")
                        .version("v1")
                        .description("API do desafio técnico - faturamento e webhooks"));
    }

    @Bean
    public GroupedOpenApi revenueGroup() {
        return GroupedOpenApi.builder()
                .group("revenue")
                .pathsToMatch("/revenue/**")
                .build();
    }
}
