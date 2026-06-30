package com.example.financetracker.report.service;

import com.example.financetracker.report.api.dto.ReportRequest;
import com.example.financetracker.report.api.error.InvalidReportPeriodException;
import com.example.financetracker.report.client.FinanceOperationsClient;
import com.example.financetracker.report.client.OperationReportItem;
import com.example.financetracker.report.model.OperationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportServiceTest {

    private static final String AUTHORIZATION = "Bearer token";
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID GROUP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID FOOD_CATEGORY_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID SALARY_CATEGORY_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private final FakeFinanceOperationsClient financeOperationsClient = new FakeFinanceOperationsClient();
    private final ReportService reportService = new ReportService(financeOperationsClient);

    @Test
    void summaryCalculatesIncomeExpenseAndBalance() {
        ReportRequest request = request(null, null, null);
        financeOperationsClient.setOperations(List.of(
                operation(USER_ID, null, FOOD_CATEGORY_ID, "Food", OperationType.EXPENSE, "30.25", "USD", "2026-06-01", "Lunch"),
                operation(USER_ID, null, SALARY_CATEGORY_ID, "Salary", OperationType.INCOME, "100.00", "USD", "2026-06-02", "Salary")
        ));

        var response = reportService.getSummaryReport(request, AUTHORIZATION);

        assertThat(response.totalIncome()).isEqualByComparingTo("100.00");
        assertThat(response.totalExpense()).isEqualByComparingTo("30.25");
        assertThat(response.balance()).isEqualByComparingTo("69.75");
        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    void byCategoryGroupsByCategoryAndType() {
        ReportRequest request = request(null, null, null);
        financeOperationsClient.setOperations(List.of(
                operation(USER_ID, null, FOOD_CATEGORY_ID, "Food", OperationType.EXPENSE, "10.00", "USD", "2026-06-01", "Lunch"),
                operation(USER_ID, null, FOOD_CATEGORY_ID, "Food", OperationType.EXPENSE, "15.50", "USD", "2026-06-02", "Dinner"),
                operation(USER_ID, null, SALARY_CATEGORY_ID, "Salary", OperationType.INCOME, "100.00", "USD", "2026-06-03", "Pay")
        ));

        var response = reportService.getCategoryReport(request, AUTHORIZATION);

        assertThat(response).hasSize(2);
        var food = response.stream()
                .filter(item -> item.categoryName().equals("Food"))
                .findFirst()
                .orElseThrow();
        var salary = response.stream()
                .filter(item -> item.categoryName().equals("Salary"))
                .findFirst()
                .orElseThrow();
        assertThat(food.totalAmount()).isEqualByComparingTo("25.50");
        assertThat(food.operationCount()).isEqualTo(2);
        assertThat(salary.totalAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void byMemberRequiresGroupAndCalculatesPerMemberBalance() {
        ReportRequest request = request(GROUP_ID, null, null);
        financeOperationsClient.setOperations(List.of(
                operation(USER_ID, GROUP_ID, FOOD_CATEGORY_ID, "Food", OperationType.EXPENSE, "20.00", "USD", "2026-06-01", "Lunch"),
                operation(USER_ID, GROUP_ID, SALARY_CATEGORY_ID, "Salary", OperationType.INCOME, "50.00", "USD", "2026-06-02", "Bonus"),
                operation(SECOND_USER_ID, GROUP_ID, FOOD_CATEGORY_ID, "Food", OperationType.EXPENSE, "7.00", "USD", "2026-06-03", "Coffee")
        ));

        var response = reportService.getMemberReport(request, AUTHORIZATION);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).userId()).isEqualTo(USER_ID);
        assertThat(response.get(0).balance()).isEqualByComparingTo("30.00");
        assertThat(response.get(0).operationCount()).isEqualTo(2);
        assertThat(response.get(1).userId()).isEqualTo(SECOND_USER_ID);
        assertThat(response.get(1).balance()).isEqualByComparingTo("-7.00");
    }

    @Test
    void byMemberWithoutGroupFails() {
        ReportRequest request = request(null, null, null);

        assertThatThrownBy(() -> reportService.getMemberReport(request, AUTHORIZATION))
                .isInstanceOf(InvalidReportPeriodException.class)
                .hasMessage("groupId is required for member report");
    }

    @Test
    void summaryWithoutCurrencyRejectsMixedCurrencies() {
        ReportRequest request = request(null, null, null);
        financeOperationsClient.setOperations(List.of(
                operation(USER_ID, null, FOOD_CATEGORY_ID, "Food", OperationType.EXPENSE, "10.00", "USD", "2026-06-01", "Lunch"),
                operation(USER_ID, null, FOOD_CATEGORY_ID, "Food", OperationType.EXPENSE, "10.00", "EUR", "2026-06-02", "Lunch")
        ));

        assertThatThrownBy(() -> reportService.getSummaryReport(request, AUTHORIZATION))
                .isInstanceOf(InvalidReportPeriodException.class)
                .hasMessage("Report contains multiple currencies; pass currency parameter to build a single-currency report");
    }

    @Test
    void exportCsvSortsAndEscapesValues() {
        ReportRequest request = request(null, null, null);
        UUID laterOperationId = UUID.fromString("30000000-0000-0000-0000-000000000002");
        UUID earlierOperationId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        financeOperationsClient.setOperations(List.of(
                operation(laterOperationId, USER_ID, null, FOOD_CATEGORY_ID, "Food, Home", OperationType.EXPENSE, "10.00", "USD", "2026-06-02", "with \"quote\""),
                operation(earlierOperationId, USER_ID, null, SALARY_CATEGORY_ID, "Salary", OperationType.INCOME, "100.00", "USD", "2026-06-01", "Pay")
        ));

        String csv = reportService.exportCsv(request, AUTHORIZATION);

        assertThat(csv).isEqualTo(
                "date,type,amount,currency,category,userId,groupId,description\n"
                        + "2026-06-01,INCOME,100.00,USD,Salary,00000000-0000-0000-0000-000000000001,,Pay\n"
                        + "2026-06-02,EXPENSE,10.00,USD,\"Food, Home\",00000000-0000-0000-0000-000000000001,,\"with \"\"quote\"\"\"\n"
        );
    }

    @Test
    void invalidPeriodFailsBeforeCallingFinance() {
        ReportRequest request = new ReportRequest(
                LocalDate.parse("2026-06-30"),
                LocalDate.parse("2026-06-01"),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> reportService.getSummaryReport(request, AUTHORIZATION))
                .isInstanceOf(InvalidReportPeriodException.class)
                .hasMessage("from must be less than or equal to to");
    }

    private ReportRequest request(UUID groupId, List<UUID> userIds, OperationType type) {
        return new ReportRequest(
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                groupId,
                userIds,
                type,
                "usd"
        );
    }

    private OperationReportItem operation(
            UUID userId,
            UUID groupId,
            UUID categoryId,
            String categoryName,
            OperationType type,
            String amount,
            String currency,
            String date,
            String description
    ) {
        return operation(UUID.randomUUID(), userId, groupId, categoryId, categoryName, type, amount, currency, date, description);
    }

    private OperationReportItem operation(
            UUID operationId,
            UUID userId,
            UUID groupId,
            UUID categoryId,
            String categoryName,
            OperationType type,
            String amount,
            String currency,
            String date,
            String description
    ) {
        return new OperationReportItem(
                operationId,
                userId,
                groupId,
                categoryId,
                categoryName,
                type,
                new BigDecimal(amount),
                currency,
                LocalDate.parse(date),
                description
        );
    }

    private static class FakeFinanceOperationsClient extends FinanceOperationsClient {

        private List<OperationReportItem> operations = new ArrayList<>();

        FakeFinanceOperationsClient() {
            super(null, null);
        }

        void setOperations(List<OperationReportItem> operations) {
            this.operations = operations;
        }

        @Override
        public List<OperationReportItem> getOperations(ReportRequest request, String authorizationHeader) {
            return operations;
        }
    }
}
