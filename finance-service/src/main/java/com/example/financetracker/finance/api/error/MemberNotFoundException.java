package com.example.financetracker.finance.api.error;

import java.util.UUID;

public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(UUID groupId, UUID userId) {
        super("User '%s' is not a member of group '%s'".formatted(userId, groupId));
    }
}
