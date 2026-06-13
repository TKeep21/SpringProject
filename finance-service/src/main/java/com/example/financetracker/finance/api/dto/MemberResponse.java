package com.example.financetracker.finance.api.dto;

import com.example.financetracker.finance.group.FamilyRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Family group member response")
public record MemberResponse(
        UUID id,
        UUID groupId,
        UUID userId,
        FamilyRole role,
        Instant joinedAt
) {
}
