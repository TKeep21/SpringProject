package com.example.financetracker.report.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportSummaryResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        String currency
) {
}
