package com.example.financetracker.finance.api.error;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class OperationNotFoundException extends ApiException {

    public OperationNotFoundException(UUID operationId) {
        super(HttpStatus.NOT_FOUND, "Operation with id '%s' not found".formatted(operationId));
    }
}
