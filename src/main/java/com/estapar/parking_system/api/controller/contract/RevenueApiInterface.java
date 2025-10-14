package com.estapar.parking_system.api.controller.contract;

import com.estapar.parking_system.api.dto.RevenueDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface RevenueApiInterface {
    @Operation(
            summary = "Consulta faturamento (GET – exigência do desafio)",
            description = "GET /revenue recebe um corpo JSON. Atenção: browsers/Swagger não executam GET com body; use o POST para 'Try it out'."
    )
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RevenueDtos.RevenueResponse.class),
                    examples = @ExampleObject(value = """
            { "amount": 123.45, "currency": "BRL", "timestamp": "2025-01-01T12:00:00.000Z" }
            """)))
    @RequestBody(required = true, description = "", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RevenueDtos.RevenueRequest.class),
                    examples = @ExampleObject(value = """
            { "date": "2025-01-01", "sector": "A" }
            """)))
    ResponseEntity<RevenueDtos.RevenueResponse> getRevenue(RevenueDtos.RevenueRequest request);

    @Operation(
            summary = "Consulta faturamento (POST – compatível com Swagger UI)",
            description = "Espelho do GET para execução no Swagger UI."
    )
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RevenueDtos.RevenueResponse.class)))
    @RequestBody(required = true, description = "Mesmo payload do GET", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RevenueDtos.RevenueRequest.class)))
    ResponseEntity<RevenueDtos.RevenueResponse> postRevenue(RevenueDtos.RevenueRequest request);

}
