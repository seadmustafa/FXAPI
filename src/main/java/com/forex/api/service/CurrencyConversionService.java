package com.forex.api.service;

import com.forex.api.dto.ConversionHistoryResponse;
import com.forex.api.dto.CurrencyConversionRequest;
import com.forex.api.dto.CurrencyConversionResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface CurrencyConversionService {

    CurrencyConversionResponse convertCurrency(CurrencyConversionRequest request);

    Page<ConversionHistoryResponse> getConversionHistory(LocalDate transactionDate, int page, int size);

}
