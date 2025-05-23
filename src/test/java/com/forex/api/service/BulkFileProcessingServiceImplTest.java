package com.forex.api.service;

import com.forex.api.dto.BulkConversionResult;
import com.forex.api.dto.CurrencyConversionRequest;
import com.forex.api.dto.CurrencyConversionResponse;
import com.forex.api.exception.ForexAppException;
import com.forex.api.service.impl.BulkFileProcessingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BulkFileProcessingServiceImplTest {

    private CurrencyConversionService currencyConversionService;
    private BulkFileProcessingServiceImpl bulkFileProcessingService;

    @BeforeEach
    void setUp() {
        currencyConversionService = mock(CurrencyConversionService.class);
        bulkFileProcessingService = new BulkFileProcessingServiceImpl(currencyConversionService);
    }

    @Test
    void testProcessBulkConversion_successfulFile() {
        // Given
        String csvData = "transactionId,sourceCurrency,targetCurrency,amount,conversionDate\n" +
                "tx001,USD,EUR,100.00,2024-01-10T10:00:00";
        MockMultipartFile file = new MockMultipartFile("file", "conversions.csv", "text/csv",
                csvData.getBytes(StandardCharsets.UTF_8));

        CurrencyConversionResponse mockResponse = CurrencyConversionResponse.builder()
                .convertedAmount(BigDecimal.valueOf(91.00))
                .conversionDate(LocalDateTime.parse("2024-01-10T10:00:00"))
                .build();

        when(currencyConversionService.convertCurrency(any(CurrencyConversionRequest.class)))
                .thenReturn(mockResponse);

        // When
        BulkConversionResult result = bulkFileProcessingService.processBulkConversion(file);

        // Then
        assertEquals(1, result.getTotalRecords());
        assertEquals(1, result.getSuccessfulConversions());
        assertEquals(0, result.getFailedConversions());
        assertEquals(BigDecimal.valueOf(91.00), result.getResults().get(0).getConvertedAmount());
        assertTrue(result.getResults().get(0).isSuccess());
    }

    @Test
    void testProcessBulkConversion_withInvalidAmount_shouldFailGracefully() {
        // Given
        String csvData = "transactionId,sourceCurrency,targetCurrency,amount,conversionDate\n" +
                "tx002,USD,EUR,-10.00,2024-01-10T10:00:00";
        MockMultipartFile file = new MockMultipartFile("file", "invalid.csv", "text/csv",
                csvData.getBytes(StandardCharsets.UTF_8));

        // When
        BulkConversionResult result = bulkFileProcessingService.processBulkConversion(file);

        // Then
        assertEquals(1, result.getTotalRecords());
        assertEquals(0, result.getSuccessfulConversions());
        assertEquals(1, result.getFailedConversions());
        assertFalse(result.getResults().get(0).isSuccess());
        assertEquals("Amount must be positive", result.getResults().get(0).getErrorMessage());
    }

    @Test
    void testProcessBulkConversion_emptyFile_shouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/csv", new byte[0]);

        ForexAppException exception = assertThrows(ForexAppException.class, () ->
                bulkFileProcessingService.processBulkConversion(emptyFile));

        assertEquals("Uploaded file is empty or missing.", exception.getMessage());
    }

    @Test
    void testProcessBulkConversion_conversionServiceThrowsException_shouldMarkAsFailed() {
        // Given
        String csvData = "transactionId,sourceCurrency,targetCurrency,amount,conversionDate\n" +
                "tx003,USD,EUR,100.00,2024-01-10T10:00:00";
        MockMultipartFile file = new MockMultipartFile("file", "error.csv", "text/csv",
                csvData.getBytes(StandardCharsets.UTF_8));

        when(currencyConversionService.convertCurrency(any(CurrencyConversionRequest.class)))
                .thenThrow(new ForexAppException("Conversion failed"));

        // When
        BulkConversionResult result = bulkFileProcessingService.processBulkConversion(file);

        // Then
        assertEquals(1, result.getTotalRecords());
        assertEquals(0, result.getSuccessfulConversions());
        assertEquals(1, result.getFailedConversions());
        assertFalse(result.getResults().get(0).isSuccess());
        assertEquals("Conversion failed", result.getResults().get(0).getErrorMessage());
    }
}
