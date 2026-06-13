package com.example.financetracker.finance.api.dto;

import com.example.financetracker.finance.category.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request to create a category")
public record CreateCategoryRequest(
        @NotBlank String name,
        @NotNull OperationType type,
        UUID groupId
) {
}
