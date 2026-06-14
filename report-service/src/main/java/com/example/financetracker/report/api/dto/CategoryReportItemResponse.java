package com.example.financetracker.report.api.dto;

import com.example.financetracker.report.model.OperationType;
import java.math.BigDecimal;
import java.util.UUID;

public record CategoryReportItemResponse(
        UUID categoryId,
        String categoryName,
        OperationType type,
        BigDecimal totalAmount,
        long operationCount
) {
}
