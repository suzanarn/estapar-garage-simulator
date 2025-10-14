package com.estapar.parking_system.api.controller;

import com.estapar.parking_system.api.dto.RevenueDtos.RevenueRequest;
import com.estapar.parking_system.api.dto.RevenueDtos.RevenueResponse;

import com.estapar.parking_system.application.service.RevenueService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/revenue")
@AllArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    @GetMapping
    public ResponseEntity<RevenueResponse> getRevenue(@RequestBody RevenueRequest request) {
        var response = revenueService.revenueForDate(request.date(), request.sector());
        return ResponseEntity.ok(response);
    }
}
