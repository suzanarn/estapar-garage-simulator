package com.estapar.parking_system.infrastructure.bootstrap;

import com.estapar.parking_system.api.dto.GarageDtos.GarageResponse;
import com.estapar.parking_system.infrastructure.client.GarageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GarageBootstrap {
    private static final Logger log = LoggerFactory.getLogger(GarageBootstrap.class);

    @Bean
    @ConditionalOnProperty(prefix = "app.bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner bootstrapGarage(GarageClient client, GarageSynchronizer sync) {
        return args -> {
            log.info("Fetching garage from simulator at {}", client.getBaseUrl());
            try {
                GarageResponse res = client.fetchGarage();
                sync.sync(res);
                log.info("Garage sync completed.");
            } catch (Exception e) {
                log.warn("Garage sync skipped: simulator unavailable or invalid response. Base={}", client.getBaseUrl(), e);
            }
        };
    }
}
