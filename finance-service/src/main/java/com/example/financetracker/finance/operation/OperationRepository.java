package com.example.financetracker.finance.operation;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OperationRepository extends JpaRepository<Operation, UUID>, JpaSpecificationExecutor<Operation> {

    Page<Operation> findAllByUserId(UUID userId, Pageable pageable);

    Page<Operation> findAllByGroupId(UUID groupId, Pageable pageable);

    boolean existsByCategoryId(UUID categoryId);
}
