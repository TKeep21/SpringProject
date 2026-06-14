package com.example.financetracker.finance.category;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByIsDefaultTrueAndTypeOrderByNameAsc(OperationType type);

    List<Category> findAllByIsDefaultTrueOrderByTypeAscNameAsc();

    List<Category> findAllByOwnerUserIdAndGroupIdIsNullOrderByNameAsc(UUID ownerUserId);

    List<Category> findAllByOwnerUserIdAndGroupIdIsNullAndTypeOrderByNameAsc(UUID ownerUserId, OperationType type);

    List<Category> findAllByGroupIdOrderByNameAsc(UUID groupId);

    List<Category> findAllByGroupIdAndTypeOrderByNameAsc(UUID groupId, OperationType type);

    boolean existsByNameIgnoreCaseAndTypeAndOwnerUserIdAndGroupId(
            String name,
            OperationType type,
            UUID ownerUserId,
            UUID groupId
    );
}
