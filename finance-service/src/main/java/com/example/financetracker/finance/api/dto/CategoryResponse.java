package com.example.financetracker.finance.api.dto;

import com.example.financetracker.finance.category.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Category response")
public record CategoryResponse(
        UUID id,
        String name,
        OperationType type,
        UUID ownerUserId,
        UUID groupId,
        boolean isDefault,
        Instant createdAt
) {
}
