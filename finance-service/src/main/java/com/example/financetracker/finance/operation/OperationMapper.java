package com.example.financetracker.finance.operation;

import com.example.financetracker.finance.api.dto.OperationResponse;
import org.springframework.stereotype.Component;

@Component
public class OperationMapper {

    public OperationResponse toResponse(Operation operation) {
        return new OperationResponse(
                operation.getId(),
                operation.getUserId(),
                operation.getGroupId(),
                operation.getCategoryId(),
                operation.getType(),
                operation.getAmount(),
                operation.getCurrency(),
                operation.getOperationDate(),
                operation.getDescription(),
                operation.getCreatedAt(),
                operation.getUpdatedAt()
        );
    }
}
