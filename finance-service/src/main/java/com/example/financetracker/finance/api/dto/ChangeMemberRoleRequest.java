package com.example.financetracker.finance.api.dto;

import com.example.financetracker.finance.group.FamilyRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to change a family group member role")
public record ChangeMemberRoleRequest(
        @NotNull FamilyRole role
) {
}
