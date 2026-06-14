package com.example.financetracker.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal")
public record InternalServiceProperties(String serviceToken) {
}
