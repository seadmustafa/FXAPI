package com.forex.api.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.forex.api.dto.CurrencyConversionRequest;
import com.forex.api.dto.CurrencyConversionResponse;
import com.forex.api.service.CurrencyConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrencyConversionControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CurrencyConversionService conversionService;
    private CurrencyConversionController currencyConversionController;

    @BeforeEach
    void setUp() {
        // Manual mock and controller setup (explicit, non-reflective, clean)
        conversionService = mock(CurrencyConversionService.class);
        currencyConversionController = new CurrencyConversionController(conversionService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(currencyConversionController).build();
    }

    @Test
    @DisplayName("Should convert currency successfully when valid request is sent")
    void shouldConvertCurrencySuccessfully() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        CurrencyConversionRequest request = new CurrencyConversionRequest("EUR", "USD", BigDecimal.valueOf(100), now);

        CurrencyConversionResponse response = CurrencyConversionResponse.builder()
                .transactionId("txn-123")
                .sourceCurrency("EUR")
                .targetCurrency("USD")
                .exchangeRate(BigDecimal.valueOf(1.10))
                .amount(BigDecimal.valueOf(100))
                .convertedAmount(BigDecimal.valueOf(110.0))
                .conversionDate(now)
                .build();

        when(conversionService.convertCurrency(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("txn-123"))
                .andExpect(jsonPath("$.sourceCurrency").value("EUR"))
                .andExpect(jsonPath("$.targetCurrency").value("USD"))
                .andExpect(jsonPath("$.convertedAmount").value(110.0));
    }

}
