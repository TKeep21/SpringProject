package com.example.financetracker.finance.api.dto;

import com.example.financetracker.finance.category.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "Operation filter request")
public record OperationFilterRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate,
        OperationType type,
        UUID categoryId,
        UUID groupId,
        UUID userId
) {

    @AssertTrue(message = "fromDate must be less than or equal to toDate")
    public boolean isDateRangeValid() {
        return fromDate == null || toDate == null || !fromDate.isAfter(toDate);
    }
}
