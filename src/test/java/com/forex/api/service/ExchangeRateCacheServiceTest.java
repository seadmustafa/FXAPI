package com.forex.api.service;

import com.forex.api.dto.ExchangeRateResponse;
import com.forex.api.utils.FixerApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExchangeRateCacheServiceTest {

    private FixerApiClient fixerApiClient;
    private ExchangeRateCacheService cacheService;

    @BeforeEach
    public void setup() {
        fixerApiClient = mock(FixerApiClient.class);
        cacheService = new ExchangeRateCacheService(fixerApiClient);
    }

    @Test
    public void shouldReturnCachedExchangeRateResponse() {
        when(fixerApiClient.getExchangeRate("USD", "EUR"))
                .thenReturn(Map.of("EUR", BigDecimal.valueOf(0.91)));

        ExchangeRateResponse response = cacheService.getExchangeRate("USD", "EUR");

        assertThat(response.getBaseCurrency()).isEqualTo("USD");
        assertThat(response.getRates()).containsKey("EUR");
    }
}
