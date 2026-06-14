package com.example.financetracker.report.client;

import com.example.financetracker.report.model.OperationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OperationReportItem(
        UUID operationId,
        UUID userId,
        UUID groupId,
        UUID categoryId,
        String categoryName,
        OperationType type,
        BigDecimal amount,
        String currency,
        LocalDate operationDate,
        String description
) {
}
