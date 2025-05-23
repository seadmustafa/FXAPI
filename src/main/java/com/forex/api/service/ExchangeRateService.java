package com.forex.api.service;

import com.forex.api.dto.ExchangeRateResponse;

import java.util.Map;

public interface ExchangeRateService {

    ExchangeRateResponse getExchangeRate(String baseCurrency, String targetCurrency);

    Map<String, ExchangeRateResponse> getExchangeRatesForMultipleCurrencies(String baseCurrency, String... targetCurrencies);
}
