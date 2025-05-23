package com.forex.api.controller;

import com.forex.api.dto.ConversionHistoryResponse;
import com.forex.api.dto.CurrencyConversionRequest;
import com.forex.api.dto.CurrencyConversionResponse;
import com.forex.api.service.CurrencyConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/v1/convert")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CurrencyConversionController {

    private final CurrencyConversionService currencyConversionService;

    @Operation(summary = "Convert currency using the latest exchange rate")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Currency converted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid conversion request"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping
    public ResponseEntity<CurrencyConversionResponse> convertCurrency(
            @Valid @RequestBody CurrencyConversionRequest request) {
        CurrencyConversionResponse response = currencyConversionService.convertCurrency(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get paginated conversion history filtered by date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversion history retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/history")
    public ResponseEntity<Page<ConversionHistoryResponse>> getConversionHistory(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate transactionDate,
            @RequestParam int page,
            @RequestParam int size) {
        if (transactionDate == null) {
            return ResponseEntity.badRequest().build();
        }

        Page<ConversionHistoryResponse> pageResult = currencyConversionService.getConversionHistory(
                transactionDate, page, size);

        return ResponseEntity.ok(pageResult);
    }
}
