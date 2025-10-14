package com.estapar.parking_system.api.controller;

import com.estapar.parking_system.api.controller.contract.RevenueApiInterface;
import com.estapar.parking_system.api.dto.RevenueDtos.RevenueRequest;
import com.estapar.parking_system.api.dto.RevenueDtos.RevenueResponse;

import com.estapar.parking_system.application.service.RevenueService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/revenue")
@AllArgsConstructor
public class RevenueController implements RevenueApiInterface {

    private final RevenueService revenueService;

    @Override
    @GetMapping
    public ResponseEntity<RevenueResponse> getRevenue(@RequestBody RevenueRequest request) {
        return ResponseEntity.ok(revenueService.revenueForDate(request.date(), request.sector()));
    }

    @PostMapping()
    public ResponseEntity<RevenueResponse> postRevenue(@Valid @RequestBody RevenueRequest request) {
        return ResponseEntity.ok(revenueService.revenueForDate(request.date(), request.sector()));
    }
}
