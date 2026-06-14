package com.example.financetracker.report.api.dto;

import com.example.financetracker.report.model.OperationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
        OperationType type,
        @Size(min = 3, max = 3)
        @Pattern(regexp = "[A-Za-z]{3}", message = "currency must be a 3-letter ISO code")
        String currency
) {

    public String normalizedCurrency() {
        return currency == null ? null : currency.trim().toUpperCase(Locale.ROOT);
    }
}
