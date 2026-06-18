package com.example.financetracker.finance.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to create a new family group")
public record CreateGroupRequest(
        @NotBlank String name,
        String description
) {
}
