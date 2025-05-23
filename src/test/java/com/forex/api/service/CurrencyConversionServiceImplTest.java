package com.forex.api.service;


import com.forex.api.dto.ConversionHistoryResponse;
import com.forex.api.dto.CurrencyConversionRequest;
import com.forex.api.dto.CurrencyConversionResponse;
import com.forex.api.dto.ExchangeRateResponse;
import com.forex.api.entity.ConversionHistory;
import com.forex.api.exception.ForexAppException;
import com.forex.api.repository.ConversionHistoryRepository;
import com.forex.api.service.impl.CurrencyConversionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrencyConversionServiceImplTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private ConversionHistoryRepository historyRepository;

    @InjectMocks
    private CurrencyConversionServiceImpl service;

    private final LocalDate now = LocalDate.now();


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void convertCurrency_EURtoUSD_shouldReturnValidResponse() {
        // Arrange
        ExchangeRateResponse mockRateResponse = mock(ExchangeRateResponse.class);
        when(mockRateResponse.getRates()).thenReturn(Map.of("USD", BigDecimal.valueOf(1.10)));
        when(exchangeRateService.getExchangeRate("EUR", "USD")).thenReturn(mockRateResponse);

        CurrencyConversionRequest request = CurrencyConversionRequest.builder()
                .sourceCurrency("EUR")
                .targetCurrency("USD")
                .amount(BigDecimal.valueOf(100))
                .conversionDate(LocalDateTime.now())
                .build();

        // Act
        CurrencyConversionResponse response = service.convertCurrency(request);

        // Assert
        assertNotNull(response.getTransactionId());
        assertEquals("EUR", response.getSourceCurrency());
        assertEquals("USD", response.getTargetCurrency());
        assertEquals(BigDecimal.valueOf(110.00), response.getConvertedAmount());
        assertEquals(BigDecimal.valueOf(1.10), response.getExchangeRate());

        verify(historyRepository).save(any(ConversionHistory.class));
    }

    @Test
    void convertCurrency_USDtoEUR_shouldInvertRateCorrectly() {
        // Arrange
        ExchangeRateResponse mockRateResponse = mock(ExchangeRateResponse.class);
        when(mockRateResponse.getRates()).thenReturn(Map.of("USD", BigDecimal.valueOf(1.25)));
        when(exchangeRateService.getExchangeRate("EUR", "USD")).thenReturn(mockRateResponse);

        CurrencyConversionRequest request = CurrencyConversionRequest.builder()
                .sourceCurrency("USD")
                .targetCurrency("EUR")
                .amount(BigDecimal.valueOf(125))
                .conversionDate(LocalDateTime.now())
                .build();

        // Act
        CurrencyConversionResponse response = service.convertCurrency(request);

        // Assert
        assertEquals(0, response.getConvertedAmount().compareTo(BigDecimal.valueOf(100)));
    }

    @Test
    void convertCurrency_sameCurrency_shouldThrowException() {
        CurrencyConversionRequest request = CurrencyConversionRequest.builder()
                .sourceCurrency("USD")
                .targetCurrency("USD")
                .amount(BigDecimal.valueOf(100))
                .build();

        ForexAppException ex = assertThrows(ForexAppException.class, () -> service.convertCurrency(request));
        assertEquals("Source and target currencies must be different.", ex.getMessage());
    }

    @Test
    void convertCurrency_missingRate_shouldThrowException() {
        ExchangeRateResponse mockRateResponse = mock(ExchangeRateResponse.class);
        when(mockRateResponse.getRates()).thenReturn(Map.of()); // No rates returned
        when(exchangeRateService.getExchangeRate("EUR", "USD")).thenReturn(mockRateResponse);

        CurrencyConversionRequest request = CurrencyConversionRequest.builder()
                .sourceCurrency("EUR")
                .targetCurrency("USD")
                .amount(BigDecimal.valueOf(100))
                .build();

        ForexAppException ex = assertThrows(ForexAppException.class, () -> service.convertCurrency(request));
        assertTrue(ex.getMessage().contains("Exchange rate not available"));
    }


    private ConversionHistory buildSampleHistory(String id) {
        return ConversionHistory.builder()
                .transactionId(id)
                .sourceCurrency("USD")
                .targetCurrency("EUR")
                .amount(BigDecimal.valueOf(100))
                .convertedAmount(BigDecimal.valueOf(91))
                .exchangeRate(BigDecimal.valueOf(0.91))
                .conversionDate(LocalDateTime.now())
                .build();
    }

    @Test
    void getConversionHistory_withTransactionDate_shouldQueryByDate() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("conversionDate").descending());
        Page<ConversionHistory> mockPage = new PageImpl<>(List.of(buildSampleHistory("tx789")));

        when(historyRepository.findByConversionDateBetween(now.atStartOfDay(), now.atTime(LocalTime.MAX), pageable)).thenReturn(mockPage);

        // Act
        Page<ConversionHistoryResponse> result = service.getConversionHistory(now, 0, 10);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("tx789", result.getContent().get(0).getTransactionId());
    }

    @Test
    void getConversionHistory_withNoFilters_shouldThrowException() {
        ForexAppException ex = assertThrows(ForexAppException.class, () ->
                service.getConversionHistory(null, 0, 10));

        assertEquals("transactionDate must be provided", ex.getMessage());
    }

}

