package com.example.financetracker.report.api.error;

public class InvalidReportPeriodException extends RuntimeException {

    public InvalidReportPeriodException(String message) {
        super(message);
    }
}
