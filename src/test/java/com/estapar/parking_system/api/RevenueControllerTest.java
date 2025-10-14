package com.estapar.parking_system.api;

import com.estapar.parking_system.api.controller.RevenueController;
import com.estapar.parking_system.api.dto.RevenueDtos.RevenueRequest;
import com.estapar.parking_system.api.dto.RevenueDtos.RevenueResponse;
import com.estapar.parking_system.application.service.RevenueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RevenueController.class)
class RevenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private RevenueService revenueService;

    @Test
    void shouldReturnRevenueForGivenDateAndSector_usingGetWithBody() throws Exception {
        var req = new RevenueRequest("2025-01-01", "A");
        var resp = new RevenueResponse(new BigDecimal("123.45"), "BRL", "2025-01-01T12:00:00Z");

        Mockito.when(revenueService.revenueForDate("2025-01-01", "A")).thenReturn(resp);

        mockMvc.perform(get("/revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.amount", is(123.45)))
                .andExpect(jsonPath("$.currency", is("BRL")))
                .andExpect(jsonPath("$.timestamp", is("2025-01-01T12:00:00Z")));

        verify(revenueService, times(1)).revenueForDate("2025-01-01", "A");
    }

    @Test
    void shouldAllowNullSector() throws Exception {
        var req = new RevenueRequest("2025-01-01", null);
        var resp = new RevenueResponse(new BigDecimal("0.00"), "BRL", "2025-01-01T00:00:00Z");

        Mockito.when(revenueService.revenueForDate("2025-01-01", null)).thenReturn(resp);

        mockMvc.perform(get("/revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(0.00)))
                .andExpect(jsonPath("$.currency", is("BRL")));

        verify(revenueService).revenueForDate("2025-01-01", null);
    }
}

