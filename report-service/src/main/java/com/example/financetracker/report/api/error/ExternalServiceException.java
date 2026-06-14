package com.example.financetracker.report.api.error;

public class ExternalServiceException extends RuntimeException {

    private final String code;

    public ExternalServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
