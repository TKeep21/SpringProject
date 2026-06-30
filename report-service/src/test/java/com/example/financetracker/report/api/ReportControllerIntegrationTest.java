package com.example.financetracker.report.api;

import com.example.financetracker.report.api.dto.ReportRequest;
import com.example.financetracker.report.api.error.ExternalServiceException;
import com.example.financetracker.report.client.FinanceOperationsClient;
import com.example.financetracker.report.client.OperationReportItem;
import com.example.financetracker.report.model.OperationType;
import com.example.financetracker.report.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        FakeFinanceOperationsClient.operations = List.of();
        FakeFinanceOperationsClient.unavailable = false;
    }

    @Test
    void summaryReportRejectsInvalidPeriodAtApiLevel() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .param("from", "2026-06-30")
                        .param("to", "2026-06-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REPORT_PERIOD"))
                .andExpect(jsonPath("$.message").value("from must be less than or equal to to"));
    }

    @Test
    void summaryReportRejectsMixedCurrenciesWithoutCurrencyFilter() throws Exception {
        FakeFinanceOperationsClient.operations = List.of(
                operation("10.00", "USD"),
                operation("20.00", "EUR")
        );

        mockMvc.perform(get("/api/v1/reports/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REPORT_PERIOD"))
                .andExpect(jsonPath("$.message").value(
                        "Report contains multiple currencies; pass currency parameter to build a single-currency report"
                ));
    }

    @Test
    void summaryReportReturnsServiceUnavailableWhenFinanceServiceFails() throws Exception {
        FakeFinanceOperationsClient.unavailable = true;

        mockMvc.perform(get("/api/v1/reports/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30")
                        .param("currency", "USD"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("FINANCE_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Finance service is temporarily unavailable"));
    }

    private OperationReportItem operation(String amount, String currency) {
        return new OperationReportItem(
                UUID.randomUUID(),
                USER_ID,
                null,
                CATEGORY_ID,
                "Food",
                OperationType.EXPENSE,
                new BigDecimal(amount),
                currency,
                LocalDate.parse("2026-06-15"),
                "Lunch"
        );
    }

    private String bearerToken() {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .claim("userId", USER_ID.toString())
                .claim("email", "user@example.com")
                .claim("role", "ROLE_USER")
                .signWith(key)
                .compact();
        return "Bearer " + token;
    }

    @TestConfiguration
    static class TestFinanceOperationsClientConfig {

        @Bean
        @Primary
        FinanceOperationsClient financeOperationsClient() {
            return new FakeFinanceOperationsClient();
        }
    }

    private static class FakeFinanceOperationsClient extends FinanceOperationsClient {

        private static List<OperationReportItem> operations = List.of();
        private static boolean unavailable;

        FakeFinanceOperationsClient() {
            super(null, null);
        }

        @Override
        public List<OperationReportItem> getOperations(ReportRequest request, String authorizationHeader) {
            if (unavailable) {
                throw new ExternalServiceException(
                        "FINANCE_SERVICE_UNAVAILABLE",
                        "Finance service is temporarily unavailable"
                );
            }
            return operations;
        }
    }
}
