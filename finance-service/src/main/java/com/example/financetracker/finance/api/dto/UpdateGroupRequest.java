package com.example.financetracker.finance.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update a family group")
public record UpdateGroupRequest(
        @NotBlank String name,
        String description
) {
}
