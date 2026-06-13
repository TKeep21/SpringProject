package com.example.financetracker.finance.category;

import com.example.financetracker.finance.api.dto.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getOwnerUserId(),
                category.getGroupId(),
                category.isDefault(),
                category.getCreatedAt()
        );
    }
}
