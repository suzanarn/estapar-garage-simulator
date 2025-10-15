package com.estapar.parking_system.api;


import com.estapar.parking_system.api.controller.WebhookController;
import com.estapar.parking_system.api.dto.WebhookDtos;
import com.estapar.parking_system.application.service.SessionAppService;
import com.estapar.parking_system.domain.exceptions.GarageFullException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WebhookController.class)
@Import(WebhookExceptionHandler.class)
class WebhookControllerTest {

    @Autowired
    MockMvc mockMvc;

    @SuppressWarnings("deprecation")
    @MockBean
    SessionAppService service;

    @Test
    @DisplayName("ENTRY → deve retornar 200 e chamar handleEntry com DTO correto")
    void entry_ok() throws Exception {
        String body = """
            {
              "license_plate": "ZUL0001",
              "entry_time": "2025-01-01T12:00:00Z",
              "event_type": "ENTRY"
            }
            """;

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<WebhookDtos.EntryEvent> captor = ArgumentCaptor.forClass(WebhookDtos.EntryEvent.class);
        verify(service, times(1)).handleEntry(captor.capture());

        WebhookDtos.EntryEvent dto = captor.getValue();
        assertThat(dto.licensePlate()).isEqualTo("ZUL0001");
        assertThat(dto.entryTime()).isEqualTo("2025-01-01T12:00:00Z");
        assertThat(dto.eventType()).isEqualTo(WebhookDtos.EventType.ENTRY);
    }

    @Test
    @DisplayName("PARKED → deve retornar 200 e chamar handleParked com DTO correto")
    void parked_ok() throws Exception {
        String body = """
            {
              "license_plate": "ZUL0001",
              "lat": -23.561684,
              "lng": -46.655981,
              "event_type": "PARKED"
            }
            """;

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<WebhookDtos.ParkedEvent> captor = ArgumentCaptor.forClass(WebhookDtos.ParkedEvent.class);
        verify(service, times(1)).handleParked(captor.capture());

        WebhookDtos.ParkedEvent dto = captor.getValue();
        assertThat(dto.licensePlate()).isEqualTo("ZUL0001");
        assertThat(dto.lat()).isEqualByComparingTo(new BigDecimal("-23.561684"));
        assertThat(dto.lng()).isEqualByComparingTo(new BigDecimal("-46.655981"));
        assertThat(dto.eventType()).isEqualTo(WebhookDtos.EventType.PARKED);
    }

    @Test
    @DisplayName("EXIT → deve retornar 200 e chamar handleExit com DTO correto")
    void exit_ok() throws Exception {
        String body = """
            {
              "license_plate": "ZUL0001",
              "exit_time": "2025-01-01T12:40:00Z",
              "event_type": "EXIT"
            }
            """;

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<WebhookDtos.ExitEvent> captor = ArgumentCaptor.forClass(WebhookDtos.ExitEvent.class);
        verify(service, times(1)).handleExit(captor.capture());

        WebhookDtos.ExitEvent dto = captor.getValue();
        assertThat(dto.licensePlate()).isEqualTo("ZUL0001");
        assertThat(dto.exitTime()).isEqualTo("2025-01-01T12:40:00Z");
        assertThat(dto.eventType()).isEqualTo(WebhookDtos.EventType.EXIT);
    }

    @Test
    @DisplayName("ENTRY com garagem cheia → service lança GarageFullException → ainda retorna 200")
    void entry_garage_full_returns_200() throws Exception {
        doThrow(new GarageFullException("Garage is full"))
                .when(service).handleEntry(any(WebhookDtos.EntryEvent.class));

        String body = """
            {
              "license_plate": "ABC1234",
              "entry_time": "2025-01-01T10:00:00Z",
              "event_type": "ENTRY"
            }
            """;

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(service, times(1)).handleEntry(any(WebhookDtos.EntryEvent.class));
    }

    @Test
    @DisplayName("event_type inválido → IllegalArgumentException → Advice mapeia para 200")
    void invalid_event_type_returns_200() throws Exception {
        String body = """
            {
              "license_plate": "ZUL0001",
              "entry_time": "2025-01-01T12:00:00Z",
              "event_type": "WHATEVER"
            }
            """;

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verifyNoInteractions(service);
    }
}

