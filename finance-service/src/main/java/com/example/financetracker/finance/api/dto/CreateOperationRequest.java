package com.example.financetracker.finance.api.dto;

import com.example.financetracker.finance.category.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Request to create a personal or group operation")
public record CreateOperationRequest(
        UUID groupId,
        @NotNull UUID categoryId,
        @NotNull OperationType type,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull LocalDate operationDate,
        String description
) {
}
