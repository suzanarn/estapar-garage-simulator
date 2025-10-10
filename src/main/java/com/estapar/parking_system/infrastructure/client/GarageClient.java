package com.estapar.parking_system.infrastructure.client;

import com.estapar.parking_system.api.dto.GarageDtos.GarageResponse;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GarageClient {
    private final RestClient restClient;
    @Getter
    private final String baseUrl;

    public GarageClient(@Value("${simulator.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public GarageResponse fetchGarage(){
        return  restClient.get()
                .uri("garage")
                .retrieve()
                .body(GarageResponse.class);
    }

}
