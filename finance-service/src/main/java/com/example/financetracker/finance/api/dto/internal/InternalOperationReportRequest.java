package com.example.financetracker.finance.api.dto.internal;

import com.example.financetracker.finance.category.OperationType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record InternalOperationReportRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,
        UUID groupId,
        List<UUID> userIds,
        OperationType type,
        @Size(min = 3, max = 3)
        @Pattern(regexp = "[A-Za-z]{3}", message = "currency must be a 3-letter ISO code")
        String currency
) {

    @AssertTrue(message = "from must be less than or equal to to")
    public boolean isDateRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }

    public String normalizedCurrency() {
        return currency == null ? null : currency.trim().toUpperCase(Locale.ROOT);
    }
}
