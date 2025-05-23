package com.forex.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionHistoryResponse {

    private String transactionId;

    private String sourceCurrency;

    private String targetCurrency;

    private BigDecimal amount;

    private BigDecimal convertedAmount;

    private BigDecimal exchangeRate;

    private LocalDateTime conversionDate;
}