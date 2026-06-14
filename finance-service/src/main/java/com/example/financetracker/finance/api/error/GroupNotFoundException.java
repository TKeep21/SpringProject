package com.example.financetracker.finance.api.error;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class GroupNotFoundException extends ApiException {

    public GroupNotFoundException(UUID groupId) {
        super(HttpStatus.NOT_FOUND, "Group with id '%s' not found".formatted(groupId));
    }
}
