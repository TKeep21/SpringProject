package com.example.financetracker.finance.service;

import com.example.financetracker.finance.api.dto.CategoryResponse;
import com.example.financetracker.finance.api.dto.CreateCategoryRequest;
import com.example.financetracker.finance.api.error.AccessDeniedException;
import com.example.financetracker.finance.api.error.AuthenticatedUserNotAvailableException;
import com.example.financetracker.finance.api.error.CategoryAlreadyExistsException;
import com.example.financetracker.finance.api.error.GroupNotFoundException;
import com.example.financetracker.finance.category.Category;
import com.example.financetracker.finance.category.CategoryMapper;
import com.example.financetracker.finance.category.CategoryRepository;
import com.example.financetracker.finance.category.OperationType;
import com.example.financetracker.finance.group.FamilyGroupRepository;
import com.example.financetracker.finance.group.FamilyMemberRepository;
import com.example.financetracker.finance.security.AuthenticatedUser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final FamilyGroupRepository familyGroupRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(
            CategoryRepository categoryRepository,
            FamilyGroupRepository familyGroupRepository,
            FamilyMemberRepository familyMemberRepository,
            CategoryMapper categoryMapper
    ) {
        this.categoryRepository = categoryRepository;
        this.familyGroupRepository = familyGroupRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        AuthenticatedUser currentUser = getCurrentUser();
        String normalizedName = normalizeName(request.name());
        UUID groupId = request.groupId();

        Category category = new Category();
        category.setName(normalizedName);
        category.setType(request.type());
        category.setDefault(false);

        if (groupId == null) {
            ensureCategoryDoesNotExist(normalizedName, request.type(), currentUser.userId(), null);
            category.setOwnerUserId(currentUser.userId());
            category.setGroupId(null);
        } else {
            requireGroupMembership(groupId, currentUser.userId());
            ensureCategoryDoesNotExist(normalizedName, request.type(), null, groupId);
            category.setOwnerUserId(null);
            category.setGroupId(groupId);
        }

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(OperationType type, UUID groupId) {
        AuthenticatedUser currentUser = getCurrentUser();
        List<Category> categories = new ArrayList<>();

        categories.addAll(type == null
                ? categoryRepository.findAllByIsDefaultTrueOrderByTypeAscNameAsc()
                : categoryRepository.findAllByIsDefaultTrueAndTypeOrderByNameAsc(type));

        if (groupId == null) {
            categories.addAll(type == null
                    ? categoryRepository.findAllByOwnerUserIdAndGroupIdIsNullOrderByNameAsc(currentUser.userId())
                    : categoryRepository.findAllByOwnerUserIdAndGroupIdIsNullAndTypeOrderByNameAsc(currentUser.userId(), type));
        } else {
            requireGroupMembership(groupId, currentUser.userId());
            categories.addAll(type == null
                    ? categoryRepository.findAllByGroupIdOrderByNameAsc(groupId)
                    : categoryRepository.findAllByGroupIdAndTypeOrderByNameAsc(groupId, type));
        }

        return categories.stream()
                .sorted(Comparator
                        .comparing(Category::getType)
                        .thenComparing(Category::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Category::getCreatedAt))
                .map(categoryMapper::toResponse)
                .toList();
    }

    private void ensureCategoryDoesNotExist(String name, OperationType type, UUID ownerUserId, UUID groupId) {
        boolean exists = categoryRepository.existsByNameIgnoreCaseAndTypeAndOwnerUserIdAndGroupId(
                name,
                type,
                ownerUserId,
                groupId
        );
        if (exists) {
            throw new CategoryAlreadyExistsException(name, type, ownerUserId, groupId);
        }
    }

    private void requireGroupMembership(UUID groupId, UUID userId) {
        boolean groupExists = familyGroupRepository.existsById(groupId);
        if (!groupExists) {
            throw new GroupNotFoundException(groupId);
        }

        boolean isMember = familyMemberRepository.existsByGroupIdAndUserId(groupId, userId);
        if (!isMember) {
            throw new AccessDeniedException("You do not have access to categories of this group");
        }
    }

    private AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }
        throw new AuthenticatedUserNotAvailableException();
    }

    private String normalizeName(String name) {
        return name.trim();
    }
}
