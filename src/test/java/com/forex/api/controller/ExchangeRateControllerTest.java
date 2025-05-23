package com.forex.api.controller;

import com.forex.api.dto.ExchangeRateResponse;
import com.forex.api.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExchangeRateControllerTest {

    private MockMvc mockMvc;
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = mock(ExchangeRateService.class);
        ExchangeRateController controller = new ExchangeRateController(exchangeRateService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/exchange-rates/{baseCurrency}/{targetCurrency} returns exchange rate")
    void getExchangeRate_returnsExchangeRate() throws Exception {
        String baseCurrency = "USD";
        String targetCurrency = "EUR";

        ExchangeRateResponse mockResponse = ExchangeRateResponse.builder()
                .baseCurrency(baseCurrency)
                .rateTimestamp(LocalDateTime.now())
                .rates(Map.of(targetCurrency, BigDecimal.valueOf(0.85)))
                .build();

        when(exchangeRateService.getExchangeRate(eq(baseCurrency), eq(targetCurrency)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/exchange-rates/{baseCurrency}/{targetCurrency}", baseCurrency, targetCurrency)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value(baseCurrency))
                .andExpect(jsonPath("$.rates.EUR").value(0.85));
    }

    @Test
    @DisplayName("GET /api/v1/exchange-rates/{baseCurrency} returns multiple exchange rates")
    void getMultipleExchangeRates_returnsRates() throws Exception {
        String baseCurrency = "USD";
        String[] targetCurrencies = {"EUR", "GBP"};

        ExchangeRateResponse eurResponse = ExchangeRateResponse.builder()
                .baseCurrency(baseCurrency)
                .rateTimestamp(LocalDateTime.now())
                .rates(Map.of("EUR", BigDecimal.valueOf(0.85)))
                .build();

        ExchangeRateResponse gbpResponse = ExchangeRateResponse.builder()
                .baseCurrency(baseCurrency)
                .rateTimestamp(LocalDateTime.now())
                .rates(Map.of("GBP", BigDecimal.valueOf(0.75)))
                .build();

        Map<String, ExchangeRateResponse> mockMap = Map.of(
                "EUR", eurResponse,
                "GBP", gbpResponse
        );

        when(exchangeRateService.getExchangeRatesForMultipleCurrencies(eq(baseCurrency), eq(targetCurrencies)))
                .thenReturn(mockMap);

        mockMvc.perform(get("/api/v1/exchange-rates/{baseCurrency}", baseCurrency)
                        .param("targetCurrencies", targetCurrencies)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.EUR.rates.EUR").value(0.85))
                .andExpect(jsonPath("$.GBP.rates.GBP").value(0.75));
    }
}
