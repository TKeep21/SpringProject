package com.example.financetracker.finance.group;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {

    Optional<FamilyMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    List<FamilyMember> findAllByUserId(UUID userId);

    List<FamilyMember> findAllByGroupId(UUID groupId);

    List<FamilyMember> findAllByGroupIdIn(Collection<UUID> groupIds);
}
