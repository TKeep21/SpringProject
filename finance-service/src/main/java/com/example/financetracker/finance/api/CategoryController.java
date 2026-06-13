package com.example.financetracker.finance.api;

import com.example.financetracker.finance.api.dto.CategoryResponse;
import com.example.financetracker.finance.api.dto.CreateCategoryRequest;
import com.example.financetracker.finance.category.OperationType;
import com.example.financetracker.finance.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a personal or group category")
    public CategoryResponse createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @GetMapping
    @Operation(summary = "Get available categories for the current user")
    public List<CategoryResponse> getCategories(
            @RequestParam(required = false) OperationType type,
            @RequestParam(required = false) UUID groupId
    ) {
        return categoryService.getCategories(type, groupId);
    }
}
