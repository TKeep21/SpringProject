package com.example.financetracker.finance.api.dto;

import com.example.financetracker.finance.category.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Request to partially update an operation")
public record UpdateOperationRequest(
        UUID groupId,
        UUID categoryId,
        OperationType type,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @Size(min = 3, max = 3) String currency,
        LocalDate operationDate,
        String description
) {
}
