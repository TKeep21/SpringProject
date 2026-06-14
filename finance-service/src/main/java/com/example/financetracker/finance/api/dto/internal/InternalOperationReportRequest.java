package com.example.financetracker.finance.api.dto.internal;

import com.example.financetracker.finance.category.OperationType;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record InternalOperationReportRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,
        UUID groupId,
        List<UUID> userIds,
        OperationType type
) {

    @AssertTrue(message = "from must be less than or equal to to")
    public boolean isDateRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}
