package com.example.financetracker.finance.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request to transfer family group ownership")
public record TransferOwnershipRequest(
        @NotNull UUID newOwnerUserId
) {
}
