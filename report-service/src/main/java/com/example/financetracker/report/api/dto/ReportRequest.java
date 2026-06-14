package com.example.financetracker.report.api.dto;

import com.example.financetracker.report.model.OperationType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record ReportRequest(
        @NotNull(message = "from is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @NotNull(message = "to is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,
        UUID groupId,
        List<UUID> userIds,
        OperationType type
) {
}
