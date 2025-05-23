package com.forex.api.service.impl;

import com.forex.api.dto.BulkConversionResult;
import com.forex.api.dto.BulkConversionRowResult;
import com.forex.api.dto.CurrencyConversionRequest;
import com.forex.api.dto.CurrencyConversionResponse;
import com.forex.api.exception.ForexAppException;
import com.forex.api.service.BulkFileProcessingService;
import com.forex.api.service.CurrencyConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkFileProcessingServiceImpl implements BulkFileProcessingService {

    private final CurrencyConversionService currencyConversionService;

    @Override
    public BulkConversionResult processBulkConversion(MultipartFile file) {
        List<BulkConversionRowResult> rowResults = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        if (file == null || file.isEmpty()) {
            throw new ForexAppException("Uploaded file is empty or missing.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : csvParser) {
                String transactionId = record.get("transactionId");
                String sourceCurrency = record.get("sourceCurrency");
                String targetCurrency = record.get("targetCurrency");
                String amountStr = record.get("amount");
                String conversionDateStr = record.get("conversionDate");

                BulkConversionRowResult rowResult = new BulkConversionRowResult();
                rowResult.setTransactionId(transactionId);

                try {
                    // Validate amount
                    BigDecimal amount = new BigDecimal(amountStr);
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Amount must be positive");
                    }

                    // Parse conversion date
                    LocalDateTime conversionDate = null;
                    if (conversionDateStr != null && !conversionDateStr.isBlank()) {
                        conversionDate = LocalDateTime.parse(conversionDateStr);
                    }

                    // Prepare conversion request
                    CurrencyConversionRequest request = CurrencyConversionRequest.builder()
                            .sourceCurrency(sourceCurrency)
                            .targetCurrency(targetCurrency)
                            .conversionDate(conversionDate)
                            .amount(amount)
                            .build();

                    // Call conversion service
                    CurrencyConversionResponse response = currencyConversionService.convertCurrency(request);

                    // Success result
                    rowResult.setSuccess(true);
                    rowResult.setConvertedAmount(response.getConvertedAmount());
                    rowResult.setConversionDate(response.getConversionDate());
                    successCount++;
                    log.info("Processed transactionId={} successfully.", transactionId);

                } catch (ForexAppException | IllegalArgumentException | DateTimeParseException ex) {
                    failureCount++;
                    rowResult.setSuccess(false);
                    rowResult.setErrorMessage(ex.getMessage());
                    log.error("Error processing transactionId={}: {}", transactionId, ex.getMessage());
                }

                rowResults.add(rowResult);
            }

        } catch (Exception ex) {
            log.error("Failed to process bulk conversion file: {}", ex.getMessage(), ex);
            throw new ForexAppException("Failed to process file: " + ex.getMessage());
        }

        return BulkConversionResult.builder()
                .totalRecords(rowResults.size())
                .successfulConversions(successCount)
                .failedConversions(failureCount)
                .conversionDate(rowResults.get(0).getConversionDate())
                .results(rowResults)
                .build();
    }
}
