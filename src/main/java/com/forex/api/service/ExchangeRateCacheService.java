package com.forex.api.service;

import com.forex.api.dto.ExchangeRateResponse;
import com.forex.api.utils.FixerApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExchangeRateCacheService {

    private final FixerApiClient fixerApiClient;

    @Cacheable("exchangeRates")
    public ExchangeRateResponse getExchangeRate(String baseCurrency, String targetCurrency) {
        Map<String, BigDecimal> rates = fixerApiClient.getExchangeRate(baseCurrency, targetCurrency);

        return ExchangeRateResponse.builder()
                .baseCurrency(baseCurrency)
                .rateTimestamp(LocalDateTime.now())
                .rates(rates)
                .build();
    }
}

