package com.forex.api.service.impl;

import com.forex.api.dto.ConversionHistoryResponse;
import com.forex.api.dto.CurrencyConversionRequest;
import com.forex.api.dto.CurrencyConversionResponse;
import com.forex.api.entity.ConversionHistory;
import com.forex.api.exception.ForexAppException;
import com.forex.api.repository.ConversionHistoryRepository;
import com.forex.api.service.CurrencyConversionService;
import com.forex.api.service.ExchangeRateService;
import com.forex.api.utils.UUIDGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private final ExchangeRateService exchangeRateService;
    private final ConversionHistoryRepository historyRepository;

    @Override
    @Transactional
    public CurrencyConversionResponse convertCurrency(CurrencyConversionRequest request) {
        String source = request.getSourceCurrency().toUpperCase();
        String target = request.getTargetCurrency().toUpperCase();

        if (source.equals(target)) {
            throw new ForexAppException("Source and target currencies must be different.", HttpStatus.BAD_REQUEST);
        }

        BigDecimal rate;

        if ("EUR".equals(source)) {
            // Direct EUR → target
            rate = getRateFromEuro(target);
        } else if ("EUR".equals(target)) {
            // Convert source → EUR (invert)
            BigDecimal sourceRate = getRateFromEuro(source);
            rate = BigDecimal.ONE.divide(sourceRate, 6, RoundingMode.HALF_UP);
        } else {
            // source → EUR → target
            BigDecimal sourceRate = getRateFromEuro(source);
            BigDecimal targetRate = getRateFromEuro(target);
            rate = targetRate.divide(sourceRate, 6, RoundingMode.HALF_UP);
        }

        BigDecimal convertedAmount = request.getAmount().multiply(rate);
        String transactionId = UUIDGenerator.generateTransactionId();

        ConversionHistory history = ConversionHistory.builder()
                .transactionId(transactionId)
                .sourceCurrency(source)
                .targetCurrency(target)
                .amount(request.getAmount())
                .convertedAmount(convertedAmount)
                .exchangeRate(rate)
                .conversionDate(request.getConversionDate())
                .build();

        historyRepository.save(history);

        return CurrencyConversionResponse.builder()
                .transactionId(transactionId)
                .sourceCurrency(source)
                .targetCurrency(target)
                .amount(request.getAmount())
                .convertedAmount(convertedAmount)
                .exchangeRate(rate)
                .conversionDate(history.getConversionDate())
                .build();
    }

    private BigDecimal getRateFromEuro(String currency) {
        var rates = exchangeRateService.getExchangeRate("EUR", currency).getRates();
        BigDecimal rate = rates.get(currency);
        if (rate == null) {
            throw new ForexAppException("Exchange rate not available for currency: " + currency, HttpStatus.BAD_REQUEST);
        }
        return rate;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversionHistoryResponse> getConversionHistory(LocalDate transactionDate, int page, int size) {
        if (transactionDate == null) {
            throw new ForexAppException("transactionDate must be provided", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime startOfDay = transactionDate.atStartOfDay();
        LocalDateTime endOfDay = transactionDate.atTime(LocalTime.MAX);

        Pageable pageable = PageRequest.of(page, size, Sort.by("conversionDate").descending());

        Page<ConversionHistory> historyPage = historyRepository.findByConversionDateBetween(startOfDay, endOfDay, pageable);

        return historyPage.map(this::mapToResponse);
    }


    private ConversionHistoryResponse mapToResponse(ConversionHistory entity) {
        return ConversionHistoryResponse.builder()
                .transactionId(entity.getTransactionId())
                .sourceCurrency(entity.getSourceCurrency())
                .targetCurrency(entity.getTargetCurrency())
                .amount(entity.getAmount())
                .convertedAmount(entity.getConvertedAmount())
                .exchangeRate(entity.getExchangeRate())
                .conversionDate(entity.getConversionDate())
                .build();
    }

}
