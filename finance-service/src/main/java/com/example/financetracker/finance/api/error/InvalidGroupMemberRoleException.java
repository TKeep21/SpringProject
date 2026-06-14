package com.example.financetracker.finance.api.error;

import org.springframework.http.HttpStatus;

public class InvalidGroupMemberRoleException extends ApiException {

    public InvalidGroupMemberRoleException() {
        super(HttpStatus.BAD_REQUEST, "New members cannot be added with OWNER role");
    }
}
