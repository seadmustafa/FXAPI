package com.forex.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkConversionResult {

    private int totalRecords;
    private int successfulConversions;
    private int failedConversions;
    private LocalDateTime conversionDate;
    private List<BulkConversionRowResult> results;
    private List<CurrencyConversionResponse> successfulConversionsList;
    private List<String> failedConversionsList;

}
