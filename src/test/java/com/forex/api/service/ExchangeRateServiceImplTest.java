package com.forex.api.service;

import com.forex.api.dto.ExchangeRateResponse;
import com.forex.api.exception.ForexAppException;
import com.forex.api.service.impl.ExchangeRateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;

class ExchangeRateServiceImplTest {

    @Mock
    private ExchangeRateCacheService cacheService;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("getExchangeRate returns rate 1 when source and target currencies are the same")
    void getExchangeRate_sameCurrency_returnsOne() {
        String currency = "USD";

        ExchangeRateResponse response = exchangeRateService.getExchangeRate(currency, currency);

        assertThat(response).isNotNull();
        assertThat(response.getBaseCurrency()).isEqualTo(currency);
        assertThat(response.getRates()).containsEntry(currency, BigDecimal.ONE);
        assertThat(response.getRateTimestamp()).isNull();
        verifyNoInteractions(cacheService);
    }

    @Test
    @DisplayName("getExchangeRate calculates cross rate correctly when valid rates exist")
    void getExchangeRate_validRates_calculatesCrossRate() {
        String source = "USD";
        String target = "GBP";

        ExchangeRateResponse eurToSource = ExchangeRateResponse.builder()
                .baseCurrency("EUR")
                .rateTimestamp(LocalDateTime.now())
                .rates(Map.of(source, new BigDecimal("1.10")))
                .build();

        ExchangeRateResponse eurToTarget = ExchangeRateResponse.builder()
                .baseCurrency("EUR")
                .rateTimestamp(LocalDateTime.now())
                .rates(Map.of(target, new BigDecimal("0.85")))
                .build();

        when(cacheService.getExchangeRate("EUR", source)).thenReturn(eurToSource);
        when(cacheService.getExchangeRate("EUR", target)).thenReturn(eurToTarget);

        ExchangeRateResponse response = exchangeRateService.getExchangeRate(source, target);

        BigDecimal expectedRate = new BigDecimal("0.85").divide(new BigDecimal("1.10"), 6, BigDecimal.ROUND_HALF_UP);

        assertThat(response).isNotNull();
        assertThat(response.getBaseCurrency()).isEqualTo(source);
        assertThat(response.getRates()).containsEntry(target, expectedRate);
        assertThat(response.getRateTimestamp()).isEqualTo(eurToTarget.getRateTimestamp());

        verify(cacheService, times(1)).getExchangeRate("EUR", source);
        verify(cacheService, times(1)).getExchangeRate("EUR", target);
    }

    @Test
    @DisplayName("getExchangeRate throws ForexAppException when rate missing")
    void getExchangeRate_missingRate_throwsForexAppException() {
        String source = "USD";
        String target = "GBP";

        ExchangeRateResponse eurToSource = ExchangeRateResponse.builder()
                .baseCurrency("EUR")
                .rateTimestamp(LocalDateTime.now())
                .rates(Map.of()) // missing source rate
                .build();

        ExchangeRateResponse eurToTarget = ExchangeRateResponse.builder()
                .baseCurrency("EUR")
                .rateTimestamp(LocalDateTime.now())
                .rates(Map.of(target, new BigDecimal("0.85")))
                .build();

        when(cacheService.getExchangeRate("EUR", source)).thenReturn(eurToSource);
        when(cacheService.getExchangeRate("EUR", target)).thenReturn(eurToTarget);

        ForexAppException thrown = catchThrowableOfType(
                () -> exchangeRateService.getExchangeRate(source, target),
                ForexAppException.class);

        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("Exchange rate not available");
        assertThat(thrown.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("getExchangeRate throws ForexAppException on unexpected exception")
    void getExchangeRate_unexpectedException_throwsForexAppException() {
        String source = "USD";
        String target = "GBP";

        when(cacheService.getExchangeRate("EUR", source)).thenThrow(new RuntimeException("Unexpected error"));

        ForexAppException thrown = catchThrowableOfType(
                () -> exchangeRateService.getExchangeRate(source, target),
                ForexAppException.class);

        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).isEqualTo("Failed to calculate exchange rate.");
        assertThat(thrown.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Test
    @DisplayName("getExchangeRatesForMultipleCurrencies returns map of multiple exchange rates")
    void getExchangeRatesForMultipleCurrencies_returnsMultipleRates() {
        String baseCurrency = "usd";

        ExchangeRateResponse rateEurToUsd = ExchangeRateResponse.builder()
                .baseCurrency("EUR")
                .rateTimestamp(LocalDateTime.now())
                .rates(Map.of("USD", BigDecimal.valueOf(1.10)))
                .build();

        ExchangeRateResponse rateEurToGbp = ExchangeRateResponse.builder()
                .baseCurrency("EUR")
                .rateTimestamp(LocalDateTime.now())
                .rates(Map.of("GBP", BigDecimal.valueOf(0.85)))
                .build();

        // Mock cacheService calls that service uses internally
        when(cacheService.getExchangeRate("EUR", "USD")).thenReturn(rateEurToUsd);
        when(cacheService.getExchangeRate("EUR", "GBP")).thenReturn(rateEurToGbp);

        // Call actual service method (not mocked)
        Map<String, ExchangeRateResponse> rates = exchangeRateService.getExchangeRatesForMultipleCurrencies(baseCurrency, "USD", "GBP");

        assertThat(rates).hasSize(2);
        assertThat(rates).containsKeys("USD", "GBP");
        // Calculate expected cross rates because service calculates cross rates internally
        BigDecimal expectedUsdRate = BigDecimal.ONE; // USD to USD = 1 (same currency)
        BigDecimal expectedGbpRate = new BigDecimal("0.85").divide(new BigDecimal("1.10"), 6, RoundingMode.HALF_UP);

        assertThat(rates.get("USD").getRates()).containsEntry("USD", expectedUsdRate);
        assertThat(rates.get("GBP").getRates()).containsEntry("GBP", expectedGbpRate);

        verify(cacheService, times(1)).getExchangeRate("EUR", "USD");
        verify(cacheService, times(1)).getExchangeRate("EUR", "GBP");
    }


}
