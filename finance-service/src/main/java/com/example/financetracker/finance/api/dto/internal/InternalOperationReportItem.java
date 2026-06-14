package com.example.financetracker.finance.api.dto.internal;

import com.example.financetracker.finance.category.OperationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InternalOperationReportItem(
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
