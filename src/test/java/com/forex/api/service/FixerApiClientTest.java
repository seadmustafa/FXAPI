package com.forex.api.service;

import com.forex.api.config.ExternalApiConfig;
import com.forex.api.exception.ForexAppException;
import com.forex.api.utils.FixerApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class FixerApiClientTest {

    @Mock
    private ExternalApiConfig externalApiConfig;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FixerApiClient fixerApiClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Replace default RestTemplate with mocked one (since original code creates a new RestTemplate internally)
        // We will use reflection to set it for testing purposes.
        java.lang.reflect.Field field;
        try {
            field = FixerApiClient.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            field.set(fixerApiClient, restTemplate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String baseUrl = "http://fake-fixer.io/api/latest";
    private String apiKey = "fake-key";

    @Test
    @DisplayName("Successfully parses rates from valid response")
    void getExchangeRate_success() {
        when(externalApiConfig.getUrl()).thenReturn(baseUrl);
        when(externalApiConfig.getApiKey()).thenReturn(apiKey);

        Map<String, Object> ratesMap = new HashMap<>();
        ratesMap.put("USD", "1.23");
        ratesMap.put("EUR", 0.89);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("rates", ratesMap);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(Map.class)))
                .thenReturn(responseEntity);

        Map<String, BigDecimal> result = fixerApiClient.getExchangeRate("EUR", "USD");

        assertThat(result).containsEntry("USD", new BigDecimal("1.23"));
        assertThat(result).containsEntry("EUR", new BigDecimal("0.89"));
    }

    @Test
    @DisplayName("Throws ForexAppException when response success is false and error code is rate limit (106)")
    void getExchangeRate_rateLimitRetries() {
        when(externalApiConfig.getUrl()).thenReturn(baseUrl);
        when(externalApiConfig.getApiKey()).thenReturn(apiKey);

        Map<String, Object> error = new HashMap<>();
        error.put("code", 106);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        responseBody.put("error", error);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(Map.class)))
                .thenReturn(responseEntity);

        ForexAppException ex = catchThrowableOfType(() -> fixerApiClient.getExchangeRate("EUR", "USD"), ForexAppException.class);

        assertThat(ex.getMessage()).isEqualTo("Rate limit reached");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Throws ForexAppException when response success is false with other error")
    void getExchangeRate_unsuccessfulResponse() {
        when(externalApiConfig.getUrl()).thenReturn(baseUrl);
        when(externalApiConfig.getApiKey()).thenReturn(apiKey);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(Map.class)))
                .thenReturn(responseEntity);

        ForexAppException ex = catchThrowableOfType(() -> fixerApiClient.getExchangeRate("EUR", "USD"), ForexAppException.class);

        assertThat(ex.getMessage()).isEqualTo("Fixer.io returned unsuccessful response.");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Throws ForexAppException when rates object is malformed")
    void getExchangeRate_malformedRates() {
        when(externalApiConfig.getUrl()).thenReturn(baseUrl);
        when(externalApiConfig.getApiKey()).thenReturn(apiKey);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("rates", "invalidType");

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(Map.class)))
                .thenReturn(responseEntity);

        ForexAppException ex = catchThrowableOfType(() -> fixerApiClient.getExchangeRate("EUR", "USD"), ForexAppException.class);

        assertThat(ex.getMessage()).isEqualTo("Malformed rates data from Fixer.io");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    @DisplayName("Throws ForexAppException on HttpClientErrorException")
    void getExchangeRate_httpClientError() {
        when(externalApiConfig.getUrl()).thenReturn(baseUrl);
        when(externalApiConfig.getApiKey()).thenReturn(apiKey);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

        ForexAppException ex = catchThrowableOfType(() -> fixerApiClient.getExchangeRate("EUR", "USD"), ForexAppException.class);

        assertThat(ex.getMessage()).isEqualTo("Failed to call Fixer.io API");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    @DisplayName("Throws ForexAppException on unexpected exceptions")
    void getExchangeRate_unexpectedException() {
        when(externalApiConfig.getUrl()).thenReturn(baseUrl);
        when(externalApiConfig.getApiKey()).thenReturn(apiKey);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(Map.class)))
                .thenThrow(new RuntimeException("Unexpected"));

        ForexAppException ex = catchThrowableOfType(() -> fixerApiClient.getExchangeRate("EUR", "USD"), ForexAppException.class);

        assertThat(ex.getMessage()).isEqualTo("Unexpected error while fetching exchange rates");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("Rethrows ForexAppException for retry to work")
    void getExchangeRate_rethrowsForexAppException() {
        when(externalApiConfig.getUrl()).thenReturn(baseUrl);
        when(externalApiConfig.getApiKey()).thenReturn(apiKey);

        // Prepare a ForexAppException to be thrown
        ForexAppException forexEx = new ForexAppException("Rate limit reached", HttpStatus.SERVICE_UNAVAILABLE);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(Map.class)))
                .thenThrow(forexEx);

        ForexAppException thrown = catchThrowableOfType(() -> fixerApiClient.getExchangeRate("EUR", "USD"), ForexAppException.class);
        assertThat(thrown).isSameAs(forexEx);
    }
}
