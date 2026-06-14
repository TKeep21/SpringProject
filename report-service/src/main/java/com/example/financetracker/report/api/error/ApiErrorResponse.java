package com.example.financetracker.report.api.error;

public record ApiErrorResponse(
        String code,
        String message
) {
}
