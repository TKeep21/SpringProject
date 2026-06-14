package com.example.financetracker.finance.api.dto;

import com.example.financetracker.finance.category.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Financial operation response")
public record OperationResponse(
        UUID id,
        UUID userId,
        UUID groupId,
        UUID categoryId,
        OperationType type,
        BigDecimal amount,
        String currency,
        LocalDate operationDate,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
