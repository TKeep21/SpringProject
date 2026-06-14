package com.example.financetracker.finance.api.error;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class MemberAlreadyExistsException extends ApiException {

    public MemberAlreadyExistsException(UUID groupId, UUID userId) {
        super(HttpStatus.CONFLICT, "User '%s' is already a member of group '%s'".formatted(userId, groupId));
    }
}
