package com.estapar.parking_system.api.controller;

import com.estapar.parking_system.api.dto.WebhookDtos;
import com.estapar.parking_system.application.service.SessionAppService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@AllArgsConstructor
public class WebhookController {
    private final SessionAppService service;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody Map<String,Object> body) {
        var type = WebhookDtos.EventType.valueOf(((String) body.get("event_type")).toUpperCase());
        System.out.println("body: " + body);
        switch (type) {
            case ENTRY -> service.handleEntry(new WebhookDtos.EntryEvent(
                    (String) body.get("license_plate"),
                    (String) body.get("entry_time"),
                    type
            ));
            case PARKED -> service.handleParked(new WebhookDtos.ParkedEvent(
                    (String) body.get("license_plate"),
                    new BigDecimal(body.get("lat").toString()),
                    new BigDecimal(body.get("lng").toString()),
                    type
            ));
            case EXIT -> service.handleExit(new WebhookDtos.ExitEvent(
                    (String) body.get("license_plate"),
                    (String) body.get("exit_time"),
                    type
            ));
        }
        return ResponseEntity.ok().build();
    }

}

