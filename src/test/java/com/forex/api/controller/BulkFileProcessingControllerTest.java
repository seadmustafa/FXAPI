package com.forex.api.controller;

import com.forex.api.dto.BulkConversionResult;
import com.forex.api.dto.BulkConversionRowResult;
import com.forex.api.exception.ForexAppException;
import com.forex.api.exception.GlobalExceptionHandler;
import com.forex.api.service.BulkFileProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BulkFileProcessingControllerTest {

    private MockMvc mockMvc;
    private BulkFileProcessingService bulkFileProcessingService;

    @BeforeEach
    void setUp() {
        bulkFileProcessingService = mock(BulkFileProcessingService.class);
        BulkFileProcessingController controller = new BulkFileProcessingController(bulkFileProcessingService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void processBulkFile_withValidCsv_shouldReturnResult() throws Exception {
        // Arrange
        String csvContent = "transactionId,sourceCurrency,targetCurrency,amount,conversionDate\n" +
                "tx001,USD,EUR,100.00,2024-01-10T10:00:00";

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "conversions.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes()
        );

        BulkConversionRowResult rowResult = new BulkConversionRowResult();
        rowResult.setTransactionId("tx001");
        rowResult.setSuccess(true);
        rowResult.setConvertedAmount(BigDecimal.valueOf(91.00));
        rowResult.setConversionDate(LocalDateTime.of(2024, 1, 10, 10, 0));

        BulkConversionResult result = BulkConversionResult.builder()
                .totalRecords(1)
                .successfulConversions(1)
                .failedConversions(0)
                .conversionDate(rowResult.getConversionDate())
                .results(List.of(rowResult))
                .build();

        when(bulkFileProcessingService.processBulkConversion(any())).thenReturn(result);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/bulk-convert")
                        .file(mockFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(1))
                .andExpect(jsonPath("$.successfulConversions").value(1))
                .andExpect(jsonPath("$.failedConversions").value(0))
                .andExpect(jsonPath("$.results[0].transactionId").value("tx001"))
                .andExpect(jsonPath("$.results[0].convertedAmount").value(91.00))
                .andExpect(jsonPath("$.results[0].success").value(true));
    }

    @Test
    void processBulkFile_whenServiceThrowsException_shouldReturnErrorJson() throws Exception {
        // Arrange
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "invalid.csv", MediaType.TEXT_PLAIN_VALUE, "bad content".getBytes()
        );

        when(bulkFileProcessingService.processBulkConversion(any()))
                .thenThrow(new ForexAppException("Invalid file format", HttpStatus.BAD_REQUEST));

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/bulk-convert")
                        .file(mockFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid file format"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/bulk-convert"));
    }
}
