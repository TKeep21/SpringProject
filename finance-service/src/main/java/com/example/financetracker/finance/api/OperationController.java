package com.example.financetracker.finance.api;

import com.example.financetracker.finance.api.dto.CreateOperationRequest;
import com.example.financetracker.finance.api.dto.OperationFilterRequest;
import com.example.financetracker.finance.api.dto.OperationResponse;
import com.example.financetracker.finance.api.dto.UpdateOperationRequest;
import com.example.financetracker.finance.service.OperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations")
@Tag(name = "Operations")
public class OperationController {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a personal or group financial operation")
    public OperationResponse createOperation(@Valid @RequestBody CreateOperationRequest request) {
        return operationService.createOperation(request);
    }

    @GetMapping
    @Operation(summary = "Get operations visible to the current user with optional filters")
    public Page<OperationResponse> getOperations(
            @ParameterObject @Valid @ModelAttribute OperationFilterRequest filter,
            @ParameterObject @PageableDefault(
                    size = 20,
                    sort = "operationDate",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return operationService.getOperations(filter, pageable);
    }

    @GetMapping("/{operationId}")
    @Operation(summary = "Get an operation by id")
    public OperationResponse getOperationById(@PathVariable UUID operationId) {
        return operationService.getOperationById(operationId);
    }

    @PatchMapping("/{operationId}")
    @Operation(summary = "Partially update an operation")
    public OperationResponse updateOperation(
            @PathVariable UUID operationId,
            @Valid @RequestBody UpdateOperationRequest request
    ) {
        return operationService.updateOperation(operationId, request);
    }

    @DeleteMapping("/{operationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an operation")
    public void deleteOperation(@PathVariable UUID operationId) {
        operationService.deleteOperation(operationId);
    }
}
