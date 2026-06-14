package com.example.financetracker.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finance-service")
public record FinanceServiceProperties(String baseUrl) {
}
