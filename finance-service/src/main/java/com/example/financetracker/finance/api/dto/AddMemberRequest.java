package com.example.financetracker.finance.api.dto;

import com.example.financetracker.finance.group.FamilyRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request to add a member to the family group")
public record AddMemberRequest(
        @NotNull UUID userId,
        @NotNull FamilyRole role
) {
}
