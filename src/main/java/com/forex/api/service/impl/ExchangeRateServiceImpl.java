package com.forex.api.service.impl;


import com.forex.api.dto.ExchangeRateResponse;
import com.forex.api.exception.ForexAppException;
import com.forex.api.service.ExchangeRateCacheService;
import com.forex.api.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private static final String EUR = "EUR";

    private final ExchangeRateCacheService cacheService;

    @Override
    public ExchangeRateResponse getExchangeRate(String sourceCurrency, String targetCurrency) {
        sourceCurrency = sourceCurrency.toUpperCase();
        targetCurrency = targetCurrency.toUpperCase();

        log.info("Calculating exchange rate from {} to {}", sourceCurrency, targetCurrency);

        if (sourceCurrency.equals(targetCurrency)) {
            // Same currency - rate is 1
            log.debug("Source and target currencies are the same. Returning rate 1.");
            Map<String, BigDecimal> rates = new HashMap<>();
            rates.put(targetCurrency, BigDecimal.ONE);
            return new ExchangeRateResponse(sourceCurrency, null, rates);
        }

        try {
            // Fetch EUR -> source and EUR -> target rates from Fixer.io
            ExchangeRateResponse eurToSource = cacheService.getExchangeRate(EUR, sourceCurrency);
            ExchangeRateResponse eurToTarget = cacheService.getExchangeRate(EUR, targetCurrency);

            BigDecimal rateEurToSource = eurToSource.getRates().get(sourceCurrency);
            BigDecimal rateEurToTarget = eurToTarget.getRates().get(targetCurrency);

            if (rateEurToSource == null || rateEurToTarget == null) {
                throw new ForexAppException("Exchange rate not available for one or both currencies.", HttpStatus.BAD_REQUEST);
            }

            // Cross rate calculation: source -> target = (EUR -> target) / (EUR -> source)
            BigDecimal crossRate = rateEurToTarget.divide(rateEurToSource, 6, RoundingMode.HALF_UP);

            Map<String, BigDecimal> rates = new HashMap<>();
            rates.put(targetCurrency, crossRate);

            log.info("Calculated cross rate from {} to {}: {}", sourceCurrency, targetCurrency, crossRate);

            return new ExchangeRateResponse(sourceCurrency, eurToTarget.getRateTimestamp(), rates);
        } catch (ForexAppException ex) {
            log.error("Error calculating exchange rate: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error calculating exchange rate", ex);
            throw new ForexAppException("Failed to calculate exchange rate.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Map<String, ExchangeRateResponse> getExchangeRatesForMultipleCurrencies(String baseCurrency, String... targetCurrencies) {
        baseCurrency = baseCurrency.toUpperCase();

        Map<String, ExchangeRateResponse> responseMap = new HashMap<>();

        for (String targetCurrency : targetCurrencies) {
            targetCurrency = targetCurrency.toUpperCase();
            ExchangeRateResponse response = getExchangeRate(baseCurrency, targetCurrency);
            responseMap.put(targetCurrency, response);
        }

        return responseMap;
    }
}
