package com.example.financetracker.finance.operation;

import com.example.financetracker.finance.api.dto.OperationFilterRequest;
import com.example.financetracker.finance.category.OperationType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class OperationSpecifications {

    private OperationSpecifications() {
    }

    public static Specification<Operation> accessibleTo(UUID currentUserId, Collection<UUID> groupIds) {
        return (root, query, criteriaBuilder) -> {
            Predicate ownPersonal = criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("userId"), currentUserId),
                    criteriaBuilder.isNull(root.get("groupId"))
            );
            if (groupIds.isEmpty()) {
                return ownPersonal;
            }
            return criteriaBuilder.or(ownPersonal, root.get("groupId").in(groupIds));
        };
    }

    public static Specification<Operation> inGroup(UUID groupId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("groupId"), groupId);
    }

    public static Specification<Operation> withFilter(OperationFilterRequest filter) {
        Specification<Operation> specification = Specification.where(null);
        specification = andIfPresent(specification, filter.fromDate(), OperationSpecifications::fromDate);
        specification = andIfPresent(specification, filter.toDate(), OperationSpecifications::toDate);
        specification = andIfPresent(specification, filter.type(), OperationSpecifications::type);
        specification = andIfPresent(specification, filter.categoryId(), OperationSpecifications::categoryId);
        specification = andIfPresent(specification, filter.userId(), OperationSpecifications::userId);
        return specification;
    }

    private static Specification<Operation> fromDate(LocalDate fromDate) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("operationDate"), fromDate);
    }

    private static Specification<Operation> toDate(LocalDate toDate) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("operationDate"), toDate);
    }

    private static Specification<Operation> type(OperationType type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("type"), type);
    }

    private static Specification<Operation> categoryId(UUID categoryId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("categoryId"), categoryId);
    }

    private static Specification<Operation> userId(UUID userId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId);
    }

    private static <T> Specification<Operation> andIfPresent(
            Specification<Operation> specification,
            T value,
            java.util.function.Function<T, Specification<Operation>> factory
    ) {
        if (value == null) {
            return specification;
        }
        return specification.and(factory.apply(value));
    }
}
