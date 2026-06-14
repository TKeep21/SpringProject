package com.example.financetracker.finance.api;

import com.example.financetracker.finance.api.dto.internal.InternalOperationReportItem;
import com.example.financetracker.finance.api.dto.internal.InternalOperationReportRequest;
import com.example.financetracker.finance.service.OperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/operations")
@Tag(name = "Internal operations")
public class InternalOperationController {

    private final OperationService operationService;

    public InternalOperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @GetMapping
    @Operation(summary = "Get operations visible to the current user for report-service")
    public List<InternalOperationReportItem> getOperationsForReports(
            @ParameterObject @Valid @ModelAttribute InternalOperationReportRequest request
    ) {
        return operationService.getOperationsForReports(request);
    }
}
