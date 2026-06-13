package com.example.financetracker.finance.api.error;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class MemberNotFoundException extends ApiException {

    public MemberNotFoundException(UUID groupId, UUID userId) {
        super(HttpStatus.NOT_FOUND, "User '%s' is not a member of group '%s'".formatted(userId, groupId));
    }
}
