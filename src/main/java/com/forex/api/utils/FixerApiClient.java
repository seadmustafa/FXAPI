package com.forex.api.utils;

import com.forex.api.config.ExternalApiConfig;
import com.forex.api.exception.ForexAppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixerApiClient {

    private final ExternalApiConfig externalApiConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    @Retryable(
            value = ForexAppException.class,
            maxAttempts = 4,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public Map<String, BigDecimal> getExchangeRate(String baseCurrency, String targetCurrency) {
        String url = String.format("%s?access_key=%s&base=%s&symbols=%s",
                externalApiConfig.getUrl(),
                externalApiConfig.getApiKey(),
                baseCurrency,
                targetCurrency);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null || !(Boolean.TRUE.equals(responseBody.get("success")))) {
                Map<String, Object> errorObj = (Map<String, Object>) responseBody.get("error");
                if (errorObj != null && (Integer) errorObj.get("code") == 106) {
                    log.warn("Rate limit reached. Will retry...");
                    throw new ForexAppException("Rate limit reached", HttpStatus.SERVICE_UNAVAILABLE);
                }
                log.error("Invalid response from Fixer.io: {}", responseBody);
                throw new ForexAppException("Fixer.io returned unsuccessful response.", HttpStatus.SERVICE_UNAVAILABLE);
            }

            Object ratesObj = responseBody.get("rates");
            if (!(ratesObj instanceof Map)) {
                throw new ForexAppException("Malformed rates data from Fixer.io", HttpStatus.BAD_GATEWAY);
            }

            Map<String, Object> rawRates = (Map<String, Object>) ratesObj;
            Map<String, BigDecimal> parsedRates = new HashMap<>();

            for (Map.Entry<String, Object> entry : rawRates.entrySet()) {
                parsedRates.put(entry.getKey(), new BigDecimal(entry.getValue().toString()));
            }

            return parsedRates;

        } catch (HttpClientErrorException ex) {
            log.error("HTTP error calling Fixer.io API: {}", ex.getMessage());
            throw new ForexAppException("Failed to call Fixer.io API", HttpStatus.BAD_GATEWAY);
        } catch (ForexAppException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Unexpected error while calling Fixer.io API: {}", ex.getMessage(), ex);
            throw new ForexAppException("Unexpected error while fetching exchange rates", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
