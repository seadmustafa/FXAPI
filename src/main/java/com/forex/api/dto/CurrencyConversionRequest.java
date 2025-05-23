package com.forex.api.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class CurrencyConversionRequest {

    @NotBlank(message = "Source currency cannot be blank.")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Invalid source currency code. Must be 3-letter ISO code.")
    private String sourceCurrency;

    @NotBlank(message = "Target currency cannot be blank.")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Invalid target currency code. Must be 3-letter ISO code.")
    private String targetCurrency;

    @NotNull(message = "Amount cannot be null.")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
    private BigDecimal amount;

    private LocalDateTime conversionDate;
}
