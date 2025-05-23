package com.forex.api.controller;

import com.forex.api.dto.ExchangeRateResponse;
import com.forex.api.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Operation(summary = "Get exchange rate for a specific currency pair")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange rate retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid currency code"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/{baseCurrency}/{targetCurrency}")
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(
            @PathVariable String baseCurrency,
            @PathVariable String targetCurrency) {
        ExchangeRateResponse response = exchangeRateService.getExchangeRate(baseCurrency, targetCurrency);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get exchange rates for multiple currencies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange rates retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid currency code"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/{baseCurrency}")
    public ResponseEntity<Map<String, ExchangeRateResponse>> getMultipleExchangeRates(
            @PathVariable String baseCurrency,
            @RequestParam String[] targetCurrencies) {
        Map<String, ExchangeRateResponse> response = exchangeRateService.getExchangeRatesForMultipleCurrencies(baseCurrency, targetCurrencies);
        return ResponseEntity.ok(response);
    }
}
