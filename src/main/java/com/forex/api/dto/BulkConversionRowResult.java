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
public class BulkConversionRowResult {
    private String transactionId;
    private boolean success;
    private BigDecimal convertedAmount;
    private String errorMessage;
    private LocalDateTime conversionDate;
}

