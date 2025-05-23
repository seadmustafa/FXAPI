package com.forex.api.controller;

import com.forex.api.dto.BulkConversionResult;
import com.forex.api.service.BulkFileProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/bulk-convert")
@RequiredArgsConstructor
public class BulkFileProcessingController {

    private final BulkFileProcessingService bulkFileProcessingService;

    @Operation(summary = "Process bulk currency conversions from a CSV file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk conversions processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file format"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkConversionResult> processBulkFile(
            @RequestParam("file") MultipartFile file) {
        BulkConversionResult result = bulkFileProcessingService.processBulkConversion(file);
        return ResponseEntity.ok(result);
    }
}

