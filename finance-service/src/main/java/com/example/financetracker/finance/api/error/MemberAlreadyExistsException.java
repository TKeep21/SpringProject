package com.example.financetracker.finance.api.error;

import java.util.UUID;

public class MemberAlreadyExistsException extends RuntimeException {

    public MemberAlreadyExistsException(UUID groupId, UUID userId) {
        super("User '%s' is already a member of group '%s'".formatted(userId, groupId));
    }
}
