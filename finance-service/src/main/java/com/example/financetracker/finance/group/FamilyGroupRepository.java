package com.example.financetracker.finance.group;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyGroupRepository extends JpaRepository<FamilyGroup, UUID> {

    List<FamilyGroup> findAllByIdIn(Collection<UUID> ids);
}
