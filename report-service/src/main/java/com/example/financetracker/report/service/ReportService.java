package com.example.financetracker.report.service;

import com.example.financetracker.report.api.dto.CategoryReportItemResponse;
import com.example.financetracker.report.api.dto.MemberReportItemResponse;
import com.example.financetracker.report.api.dto.ReportRequest;
import com.example.financetracker.report.api.dto.ReportSummaryResponse;
import com.example.financetracker.report.api.error.InvalidReportPeriodException;
import com.example.financetracker.report.client.FinanceOperationsClient;
import com.example.financetracker.report.client.OperationReportItem;
import com.example.financetracker.report.model.OperationType;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private static final String DEFAULT_CURRENCY = "USD";

    private final FinanceOperationsClient financeOperationsClient;

    public ReportService(FinanceOperationsClient financeOperationsClient) {
        this.financeOperationsClient = financeOperationsClient;
    }

    public ReportSummaryResponse getSummaryReport(ReportRequest request, String authorizationHeader) {
        validatePeriod(request);
        List<OperationReportItem> operations = financeOperationsClient.getOperations(request, authorizationHeader);
        String currency = resolveSingleCurrency(operations, request);
        BigDecimal totalIncome = sumByType(operations, OperationType.INCOME);
        BigDecimal totalExpense = sumByType(operations, OperationType.EXPENSE);
        return new ReportSummaryResponse(
                request.from(),
                request.to(),
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense),
                currency
        );
    }

    public List<CategoryReportItemResponse> getCategoryReport(ReportRequest request, String authorizationHeader) {
        validatePeriod(request);
        List<OperationReportItem> operations = financeOperationsClient.getOperations(request, authorizationHeader);
        requireSingleCurrency(operations, request);
        return operations
                .stream()
                .collect(Collectors.groupingBy(this::categoryKey))
                .entrySet()
                .stream()
                .map(entry -> toCategoryResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparing(CategoryReportItemResponse::type)
                        .thenComparing(CategoryReportItemResponse::categoryName))
                .toList();
    }

    public List<MemberReportItemResponse> getMemberReport(ReportRequest request, String authorizationHeader) {
        validatePeriod(request);
        if (request.groupId() == null) {
            throw new InvalidReportPeriodException("groupId is required for member report");
        }
        List<OperationReportItem> operations = financeOperationsClient.getOperations(request, authorizationHeader);
        requireSingleCurrency(operations, request);
        return operations
                .stream()
                .collect(Collectors.groupingBy(OperationReportItem::userId))
                .entrySet()
                .stream()
                .map(this::toMemberResponse)
                .sorted(Comparator.comparing(MemberReportItemResponse::userId))
                .toList();
    }

    public String exportCsv(ReportRequest request, String authorizationHeader) {
        validatePeriod(request);
        List<OperationReportItem> operations = financeOperationsClient.getOperations(request, authorizationHeader)
                .stream()
                .sorted(Comparator
                        .comparing(OperationReportItem::operationDate)
                        .thenComparing(OperationReportItem::operationId))
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("date,type,amount,currency,category,userId,groupId,description\n");
        operations.forEach(operation -> csv
                .append(operation.operationDate()).append(',')
                .append(operation.type()).append(',')
                .append(operation.amount()).append(',')
                .append(escape(operation.currency())).append(',')
                .append(escape(operation.categoryName())).append(',')
                .append(operation.userId()).append(',')
                .append(operation.groupId() == null ? "" : operation.groupId()).append(',')
                .append(escape(operation.description()))
                .append('\n'));
        return csv.toString();
    }

    private void validatePeriod(ReportRequest request) {
        if (request.from() == null) {
            throw new InvalidReportPeriodException("from is required");
        }
        if (request.to() == null) {
            throw new InvalidReportPeriodException("to is required");
        }
        if (request.from().isAfter(request.to())) {
            throw new InvalidReportPeriodException("from must be less than or equal to to");
        }
    }

    private BigDecimal sumByType(List<OperationReportItem> operations, OperationType type) {
        return operations.stream()
                .filter(operation -> operation.type() == type)
                .map(OperationReportItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CategoryKey categoryKey(OperationReportItem operation) {
        return new CategoryKey(operation.categoryId(), operation.categoryName(), operation.type());
    }

    private CategoryReportItemResponse toCategoryResponse(
            CategoryKey key,
            List<OperationReportItem> operations
    ) {
        BigDecimal total = operations.stream()
                .map(OperationReportItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CategoryReportItemResponse(
                key.categoryId(),
                key.categoryName(),
                key.type(),
                total,
                operations.size()
        );
    }

    private MemberReportItemResponse toMemberResponse(Map.Entry<UUID, List<OperationReportItem>> entry) {
        BigDecimal totalIncome = sumByType(entry.getValue(), OperationType.INCOME);
        BigDecimal totalExpense = sumByType(entry.getValue(), OperationType.EXPENSE);
        return new MemberReportItemResponse(
                entry.getKey(),
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense),
                entry.getValue().size()
        );
    }

    private String resolveSingleCurrency(List<OperationReportItem> operations, ReportRequest request) {
        Set<String> currencies = operations.stream()
                .map(OperationReportItem::currency)
                .collect(Collectors.toSet());
        if (currencies.size() > 1) {
            throw new InvalidReportPeriodException(
                    "Report contains multiple currencies; pass currency parameter to build a single-currency report"
            );
        }
        return currencies.stream()
                .findFirst()
                .orElse(request.normalizedCurrency() == null ? DEFAULT_CURRENCY : request.normalizedCurrency());
    }

    private void requireSingleCurrency(List<OperationReportItem> operations, ReportRequest request) {
        resolveSingleCurrency(operations, request);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private record CategoryKey(UUID categoryId, String categoryName, OperationType type) {
    }
}
