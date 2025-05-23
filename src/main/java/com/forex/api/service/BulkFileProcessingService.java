package com.forex.api.service;


import com.forex.api.dto.BulkConversionResult;
import org.springframework.web.multipart.MultipartFile;

public interface BulkFileProcessingService {

    BulkConversionResult processBulkConversion(MultipartFile file);
}
