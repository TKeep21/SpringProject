package com.example.financetracker.finance.api.error;

import com.example.financetracker.finance.category.OperationType;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CategoryAlreadyExistsException extends ApiException {

    public CategoryAlreadyExistsException(String name, OperationType type, UUID ownerUserId, UUID groupId) {
        super(HttpStatus.CONFLICT, buildMessage(name, type, ownerUserId, groupId));
    }

    private static String buildMessage(String name, OperationType type, UUID ownerUserId, UUID groupId) {
        if (groupId != null) {
            return "Category '%s' with type '%s' already exists in group '%s'".formatted(name, type, groupId);
        }
        if (ownerUserId != null) {
            return "Personal category '%s' with type '%s' already exists for user '%s'"
                    .formatted(name, type, ownerUserId);
        }
        return "Category '%s' with type '%s' already exists".formatted(name, type);
    }
}
