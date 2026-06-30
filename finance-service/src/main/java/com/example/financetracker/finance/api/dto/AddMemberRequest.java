package com.example.financetracker.finance.api.dto;

import com.example.financetracker.finance.group.FamilyRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to add a member to the family group")
public record AddMemberRequest(
        @NotBlank @Email String email,
        @NotNull FamilyRole role
) {
}
