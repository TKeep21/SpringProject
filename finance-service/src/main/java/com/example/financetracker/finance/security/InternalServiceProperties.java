package com.example.financetracker.finance.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal")
public record InternalServiceProperties(String serviceToken) {
}
