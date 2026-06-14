package com.example.financetracker.finance.service;

import com.example.financetracker.finance.api.dto.CreateOperationRequest;
import com.example.financetracker.finance.api.dto.internal.InternalOperationReportItem;
import com.example.financetracker.finance.api.dto.internal.InternalOperationReportRequest;
import com.example.financetracker.finance.api.dto.OperationFilterRequest;
import com.example.financetracker.finance.api.dto.OperationResponse;
import com.example.financetracker.finance.api.dto.UpdateOperationRequest;
import com.example.financetracker.finance.api.error.AccessDeniedException;
import com.example.financetracker.finance.api.error.AuthenticatedUserNotAvailableException;
import com.example.financetracker.finance.api.error.CategoryNotFoundException;
import com.example.financetracker.finance.api.error.GroupNotFoundException;
import com.example.financetracker.finance.api.error.InvalidOperationCategoryException;
import com.example.financetracker.finance.api.error.InvalidOperationFilterException;
import com.example.financetracker.finance.api.error.OperationNotFoundException;
import com.example.financetracker.finance.category.Category;
import com.example.financetracker.finance.category.CategoryRepository;
import com.example.financetracker.finance.category.OperationType;
import com.example.financetracker.finance.group.FamilyGroupRepository;
import com.example.financetracker.finance.group.FamilyMember;
import com.example.financetracker.finance.group.FamilyMemberRepository;
import com.example.financetracker.finance.group.FamilyRole;
import com.example.financetracker.finance.operation.Operation;
import com.example.financetracker.finance.operation.OperationMapper;
import com.example.financetracker.finance.operation.OperationRepository;
import com.example.financetracker.finance.operation.OperationSpecifications;
import com.example.financetracker.finance.security.AuthenticatedUser;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationService {

    private final OperationRepository operationRepository;
    private final CategoryRepository categoryRepository;
    private final FamilyGroupRepository familyGroupRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final OperationMapper operationMapper;

    public OperationService(
            OperationRepository operationRepository,
            CategoryRepository categoryRepository,
            FamilyGroupRepository familyGroupRepository,
            FamilyMemberRepository familyMemberRepository,
            OperationMapper operationMapper
    ) {
        this.operationRepository = operationRepository;
        this.categoryRepository = categoryRepository;
        this.familyGroupRepository = familyGroupRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.operationMapper = operationMapper;
    }

    @Transactional
    public OperationResponse createOperation(CreateOperationRequest request) {
        AuthenticatedUser currentUser = getCurrentUser();
        UUID groupId = request.groupId();
        if (groupId != null) {
            requireGroupExists(groupId);
            requireMembership(groupId, currentUser.userId());
        }

        Category category = requireValidCategory(
                request.categoryId(),
                request.type(),
                groupId,
                currentUser.userId()
        );

        Operation operation = new Operation();
        operation.setUserId(currentUser.userId());
        operation.setGroupId(groupId);
        operation.setCategoryId(category.getId());
        operation.setType(request.type());
        operation.setAmount(request.amount());
        operation.setCurrency(normalizeCurrency(request.currency()));
        operation.setOperationDate(request.operationDate());
        operation.setDescription(trimToNull(request.description()));

        return operationMapper.toResponse(operationRepository.save(operation));
    }

    @Transactional(readOnly = true)
    public Page<OperationResponse> getOperations(OperationFilterRequest filter, Pageable pageable) {
        AuthenticatedUser currentUser = getCurrentUser();
        validateUserFilter(filter, currentUser.userId());

        Specification<Operation> accessSpecification;
        if (filter.groupId() == null) {
            List<UUID> groupIds = familyMemberRepository.findAllByUserId(currentUser.userId())
                    .stream()
                    .map(FamilyMember::getGroupId)
                    .toList();
            accessSpecification = OperationSpecifications.accessibleTo(currentUser.userId(), groupIds);
        } else {
            requireGroupExists(filter.groupId());
            requireMembership(filter.groupId(), currentUser.userId());
            accessSpecification = OperationSpecifications.inGroup(filter.groupId());
        }

        Specification<Operation> specification = accessSpecification.and(OperationSpecifications.withFilter(filter));
        return operationRepository.findAll(specification, pageable).map(operationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<InternalOperationReportItem> getOperationsForReports(InternalOperationReportRequest request) {
        AuthenticatedUser currentUser = getCurrentUser();
        validateReportUserFilter(request, currentUser.userId());

        Specification<Operation> accessSpecification;
        if (request.groupId() == null) {
            List<UUID> groupIds = familyMemberRepository.findAllByUserId(currentUser.userId())
                    .stream()
                    .map(FamilyMember::getGroupId)
                    .toList();
            accessSpecification = OperationSpecifications.accessibleTo(currentUser.userId(), groupIds);
        } else {
            requireGroupExists(request.groupId());
            requireMembership(request.groupId(), currentUser.userId());
            accessSpecification = OperationSpecifications.inGroup(request.groupId());
        }

        Specification<Operation> specification = accessSpecification
                .and(reportFromDate(request))
                .and(reportToDate(request))
                .and(reportType(request))
                .and(reportUserIds(request));

        List<Operation> operations = operationRepository.findAll(
                specification,
                Sort.by(Sort.Direction.ASC, "operationDate").and(Sort.by("id"))
        );
        Map<UUID, String> categoryNames = categoryRepository.findAllById(
                        operations.stream().map(Operation::getCategoryId).distinct().toList()
                )
                .stream()
                .collect(java.util.stream.Collectors.toMap(Category::getId, Category::getName));

        return operations.stream()
                .map(operation -> new InternalOperationReportItem(
                        operation.getId(),
                        operation.getUserId(),
                        operation.getGroupId(),
                        operation.getCategoryId(),
                        categoryNames.getOrDefault(operation.getCategoryId(), "Unknown category"),
                        operation.getType(),
                        operation.getAmount(),
                        operation.getCurrency(),
                        operation.getOperationDate(),
                        operation.getDescription()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public OperationResponse getOperationById(UUID operationId) {
        Operation operation = requireOperation(operationId);
        requireCanView(operation, getCurrentUser().userId());
        return operationMapper.toResponse(operation);
    }

    @Transactional
    public OperationResponse updateOperation(UUID operationId, UpdateOperationRequest request) {
        AuthenticatedUser currentUser = getCurrentUser();
        Operation operation = requireOperation(operationId);
        requireCanManage(operation, currentUser.userId());

        UUID effectiveGroupId = request.groupId() == null ? operation.getGroupId() : request.groupId();
        OperationType effectiveType = request.type() == null ? operation.getType() : request.type();
        UUID effectiveCategoryId = request.categoryId() == null ? operation.getCategoryId() : request.categoryId();

        if (!Objects.equals(effectiveGroupId, operation.getGroupId())) {
            requireGroupExists(effectiveGroupId);
            requireMembership(effectiveGroupId, currentUser.userId());
        }

        Category category = requireValidCategory(
                effectiveCategoryId,
                effectiveType,
                effectiveGroupId,
                currentUser.userId()
        );

        operation.setGroupId(effectiveGroupId);
        operation.setCategoryId(category.getId());
        operation.setType(effectiveType);
        if (request.amount() != null) {
            operation.setAmount(request.amount());
        }
        if (request.currency() != null) {
            operation.setCurrency(normalizeCurrency(request.currency()));
        }
        if (request.operationDate() != null) {
            operation.setOperationDate(request.operationDate());
        }
        if (request.description() != null) {
            operation.setDescription(trimToNull(request.description()));
        }

        return operationMapper.toResponse(operationRepository.save(operation));
    }

    @Transactional
    public void deleteOperation(UUID operationId) {
        AuthenticatedUser currentUser = getCurrentUser();
        Operation operation = requireOperation(operationId);
        requireCanManage(operation, currentUser.userId());
        operationRepository.delete(operation);
    }

    private void validateUserFilter(OperationFilterRequest filter, UUID currentUserId) {
        if (filter.groupId() == null && filter.userId() != null && !filter.userId().equals(currentUserId)) {
            throw new InvalidOperationFilterException("Filtering by another user requires a groupId");
        }
    }

    private void validateReportUserFilter(InternalOperationReportRequest request, UUID currentUserId) {
        if (request.groupId() == null
                && request.userIds() != null
                && request.userIds().stream().anyMatch(userId -> !userId.equals(currentUserId))) {
            throw new InvalidOperationFilterException("Filtering by other users requires a groupId");
        }
    }

    private Specification<Operation> reportFromDate(InternalOperationReportRequest request) {
        if (request.from() == null) {
            return Specification.where(null);
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("operationDate"), request.from());
    }

    private Specification<Operation> reportToDate(InternalOperationReportRequest request) {
        if (request.to() == null) {
            return Specification.where(null);
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("operationDate"), request.to());
    }

    private Specification<Operation> reportType(InternalOperationReportRequest request) {
        if (request.type() == null) {
            return Specification.where(null);
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("type"), request.type());
    }

    private Specification<Operation> reportUserIds(InternalOperationReportRequest request) {
        if (request.userIds() == null || request.userIds().isEmpty()) {
            return Specification.where(null);
        }
        return (root, query, criteriaBuilder) -> root.get("userId").in(request.userIds());
    }

    private Operation requireOperation(UUID operationId) {
        return operationRepository.findById(operationId)
                .orElseThrow(() -> new OperationNotFoundException(operationId));
    }

    private Category requireValidCategory(UUID categoryId, OperationType operationType, UUID groupId, UUID currentUserId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with id '%s' not found".formatted(categoryId)
                ));

        if (category.getType() != operationType) {
            throw new InvalidOperationCategoryException("Category type must match operation type");
        }

        if (groupId == null) {
            requirePersonalCategory(category, currentUserId);
        } else {
            requireGroupCategory(category, groupId);
        }
        return category;
    }

    private void requirePersonalCategory(Category category, UUID currentUserId) {
        boolean isAllowed = category.isDefault()
                || (category.getGroupId() == null && currentUserId.equals(category.getOwnerUserId()));
        if (!isAllowed) {
            throw new InvalidOperationCategoryException(
                    "Personal operation can use only personal or default categories"
            );
        }
    }

    private void requireGroupCategory(Category category, UUID groupId) {
        boolean isAllowed = category.isDefault() || groupId.equals(category.getGroupId());
        if (!isAllowed) {
            throw new InvalidOperationCategoryException(
                    "Group operation can use only group or default categories"
            );
        }
    }

    private void requireCanView(Operation operation, UUID currentUserId) {
        if (operation.getGroupId() == null) {
            if (!operation.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("You do not have access to this operation");
            }
            return;
        }
        requireMembership(operation.getGroupId(), currentUserId);
    }

    private void requireCanManage(Operation operation, UUID currentUserId) {
        if (operation.getGroupId() == null) {
            if (!operation.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("You cannot manage another user's personal operation");
            }
            return;
        }

        FamilyMember membership = requireMembership(operation.getGroupId(), currentUserId);
        boolean ownGroupOperation = operation.getUserId().equals(currentUserId);
        boolean manager = membership.getRole() == FamilyRole.OWNER || membership.getRole() == FamilyRole.ADMIN;
        if (!ownGroupOperation && !manager) {
            throw new AccessDeniedException("Only operation author, group owner, or group admin can manage this operation");
        }
    }

    private void requireGroupExists(UUID groupId) {
        if (!familyGroupRepository.existsById(groupId)) {
            throw new GroupNotFoundException(groupId);
        }
    }

    private FamilyMember requireMembership(UUID groupId, UUID userId) {
        return familyMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new AccessDeniedException("You do not have access to this group"));
    }

    private AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }
        throw new AuthenticatedUserNotAvailableException();
    }

    private String normalizeCurrency(String currency) {
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
