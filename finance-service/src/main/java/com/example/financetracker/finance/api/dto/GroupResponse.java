package com.example.financetracker.finance.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Family group response")
public record GroupResponse(
        UUID id,
        String name,
        String description,
        UUID ownerUserId,
        Instant createdAt,
        Instant updatedAt,
        List<MemberResponse> members
) {
}
