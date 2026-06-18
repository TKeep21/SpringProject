package com.example.financetracker.finance.api.error;

import java.util.UUID;

public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException(UUID groupId) {
        super("Group with id '%s' not found".formatted(groupId));
    }
}
