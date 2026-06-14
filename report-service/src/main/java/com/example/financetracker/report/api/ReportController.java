package com.example.financetracker.report.api;

import com.example.financetracker.report.api.dto.CategoryReportItemResponse;
import com.example.financetracker.report.api.dto.MemberReportItemResponse;
import com.example.financetracker.report.api.dto.ReportRequest;
import com.example.financetracker.report.api.dto.ReportSummaryResponse;
import com.example.financetracker.report.api.error.InvalidReportPeriodException;
import com.example.financetracker.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get income, expense, and balance summary report")
    public ReportSummaryResponse getSummaryReport(
            @ParameterObject @Valid @ModelAttribute ReportRequest request,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return reportService.getSummaryReport(request, authorizationHeader);
    }

    @GetMapping("/by-category")
    @Operation(summary = "Get report grouped by category and operation type")
    public List<CategoryReportItemResponse> getCategoryReport(
            @ParameterObject @Valid @ModelAttribute ReportRequest request,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return reportService.getCategoryReport(request, authorizationHeader);
    }

    @GetMapping("/by-member")
    @Operation(summary = "Get group report grouped by member")
    public List<MemberReportItemResponse> getMemberReport(
            @ParameterObject @Valid @ModelAttribute ReportRequest request,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return reportService.getMemberReport(request, authorizationHeader);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export operations report as CSV")
    public ResponseEntity<String> exportReport(
            @ParameterObject @Valid @ModelAttribute ReportRequest request,
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw new InvalidReportPeriodException("Only csv export format is supported");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"operations-report.csv\"")
                .body(reportService.exportCsv(request, authorizationHeader));
    }
}
