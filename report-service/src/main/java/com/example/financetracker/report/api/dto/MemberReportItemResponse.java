package com.example.financetracker.report.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberReportItemResponse(
        UUID userId,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        long operationCount
) {
}
