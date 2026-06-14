package com.example.financetracker.finance.service;

import com.example.financetracker.finance.api.dto.AddMemberRequest;
import com.example.financetracker.finance.api.dto.ChangeMemberRoleRequest;
import com.example.financetracker.finance.api.dto.CreateGroupRequest;
import com.example.financetracker.finance.api.dto.GroupResponse;
import com.example.financetracker.finance.api.dto.MemberResponse;
import com.example.financetracker.finance.api.dto.TransferOwnershipRequest;
import com.example.financetracker.finance.api.dto.UpdateGroupRequest;
import com.example.financetracker.finance.api.error.AccessDeniedException;
import com.example.financetracker.finance.api.error.AuthenticatedUserNotAvailableException;
import com.example.financetracker.finance.api.error.GroupNotFoundException;
import com.example.financetracker.finance.api.error.InvalidGroupMemberRoleException;
import com.example.financetracker.finance.api.error.MemberAlreadyExistsException;
import com.example.financetracker.finance.api.error.MemberNotFoundException;
import com.example.financetracker.finance.client.AuthUsersClient;
import com.example.financetracker.finance.group.FamilyGroup;
import com.example.financetracker.finance.group.FamilyGroupRepository;
import com.example.financetracker.finance.group.FamilyMember;
import com.example.financetracker.finance.group.FamilyMemberRepository;
import com.example.financetracker.finance.group.FamilyRole;
import com.example.financetracker.finance.security.AuthenticatedUser;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FamilyGroupService {

    private final FamilyGroupRepository familyGroupRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final AuthUsersClient authUsersClient;

    public FamilyGroupService(
            FamilyGroupRepository familyGroupRepository,
            FamilyMemberRepository familyMemberRepository,
            AuthUsersClient authUsersClient
    ) {
        this.familyGroupRepository = familyGroupRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.authUsersClient = authUsersClient;
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        AuthenticatedUser currentUser = getCurrentUser();

        FamilyGroup group = new FamilyGroup();
        group.setName(request.name().trim());
        group.setDescription(trimToNull(request.description()));
        group.setOwnerUserId(currentUser.userId());
        FamilyGroup savedGroup = familyGroupRepository.save(group);

        FamilyMember ownerMembership = new FamilyMember();
        ownerMembership.setGroupId(savedGroup.getId());
        ownerMembership.setUserId(currentUser.userId());
        ownerMembership.setRole(FamilyRole.OWNER);
        familyMemberRepository.save(ownerMembership);

        return toGroupResponse(savedGroup, List.of(ownerMembership));
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getMyGroups() {
        AuthenticatedUser currentUser = getCurrentUser();
        List<FamilyMember> memberships = familyMemberRepository.findAllByUserId(currentUser.userId());
        if (memberships.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> groupIds = memberships.stream()
                .map(FamilyMember::getGroupId)
                .toList();

        List<FamilyGroup> groups = familyGroupRepository.findAllByIdIn(groupIds);
        Map<UUID, List<FamilyMember>> membersByGroupId = familyMemberRepository.findAllByGroupIdIn(groupIds)
                .stream()
                .collect(Collectors.groupingBy(FamilyMember::getGroupId));

        return groups.stream()
                .sorted(Comparator.comparing(FamilyGroup::getCreatedAt))
                .map(group -> toGroupResponse(group, membersByGroupId.getOrDefault(group.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(UUID groupId) {
        FamilyGroup group = requireGroup(groupId);
        requireMembership(groupId, getCurrentUser().userId());
        return toGroupResponse(group, familyMemberRepository.findAllByGroupId(groupId));
    }

    @Transactional
    public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request) {
        FamilyGroup group = requireGroup(groupId);
        FamilyMember actorMembership = requireMembership(groupId, getCurrentUser().userId());
        requireManager(actorMembership);

        group.setName(request.name().trim());
        group.setDescription(trimToNull(request.description()));
        FamilyGroup savedGroup = familyGroupRepository.save(group);
        return toGroupResponse(savedGroup, familyMemberRepository.findAllByGroupId(groupId));
    }

    @Transactional
    public void deleteGroup(UUID groupId) {
        FamilyGroup group = requireGroup(groupId);
        FamilyMember actorMembership = requireMembership(groupId, getCurrentUser().userId());
        requireOwner(actorMembership);
        familyGroupRepository.delete(group);
    }

    @Transactional
    public MemberResponse addMember(UUID groupId, AddMemberRequest request) {
        requireGroup(groupId);
        FamilyMember actorMembership = requireMembership(groupId, getCurrentUser().userId());
        requireManager(actorMembership);

        if (request.role() == FamilyRole.OWNER) {
            throw new InvalidGroupMemberRoleException();
        }
        authUsersClient.requireUserExists(request.userId());
        if (familyMemberRepository.existsByGroupIdAndUserId(groupId, request.userId())) {
            throw new MemberAlreadyExistsException(groupId, request.userId());
        }

        FamilyMember member = new FamilyMember();
        member.setGroupId(groupId);
        member.setUserId(request.userId());
        member.setRole(request.role());
        FamilyMember savedMember = familyMemberRepository.save(member);
        return toMemberResponse(savedMember);
    }

    @Transactional
    public void removeMember(UUID groupId, UUID userId) {
        requireGroup(groupId);
        FamilyMember actorMembership = requireMembership(groupId, getCurrentUser().userId());
        requireManager(actorMembership);

        FamilyMember targetMembership = familyMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new MemberNotFoundException(groupId, userId));

        if (targetMembership.getRole() == FamilyRole.OWNER) {
            throw new AccessDeniedException("Owner cannot be removed from the group");
        }
        if (targetMembership.getRole() == FamilyRole.ADMIN && actorMembership.getRole() != FamilyRole.OWNER) {
            throw new AccessDeniedException("Only owner can remove an admin");
        }

        familyMemberRepository.delete(targetMembership);
    }

    @Transactional
    public MemberResponse changeMemberRole(UUID groupId, UUID userId, ChangeMemberRoleRequest request) {
        requireGroup(groupId);
        FamilyMember actorMembership = requireMembership(groupId, getCurrentUser().userId());
        requireOwner(actorMembership);

        if (request.role() == FamilyRole.OWNER) {
            throw new InvalidGroupMemberRoleException();
        }

        FamilyMember targetMembership = familyMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new MemberNotFoundException(groupId, userId));
        if (targetMembership.getRole() == FamilyRole.OWNER) {
            throw new AccessDeniedException("Owner role can be transferred only through ownership transfer");
        }

        targetMembership.setRole(request.role());
        return toMemberResponse(familyMemberRepository.save(targetMembership));
    }

    @Transactional
    public GroupResponse transferOwnership(UUID groupId, TransferOwnershipRequest request) {
        FamilyGroup group = requireGroup(groupId);
        AuthenticatedUser currentUser = getCurrentUser();
        FamilyMember currentOwnerMembership = requireMembership(groupId, currentUser.userId());
        requireOwner(currentOwnerMembership);

        FamilyMember newOwnerMembership = familyMemberRepository
                .findByGroupIdAndUserId(groupId, request.newOwnerUserId())
                .orElseThrow(() -> new MemberNotFoundException(groupId, request.newOwnerUserId()));

        if (!currentUser.userId().equals(request.newOwnerUserId())) {
            currentOwnerMembership.setRole(FamilyRole.ADMIN);
            newOwnerMembership.setRole(FamilyRole.OWNER);
            group.setOwnerUserId(request.newOwnerUserId());
            familyMemberRepository.save(currentOwnerMembership);
            familyMemberRepository.save(newOwnerMembership);
            familyGroupRepository.save(group);
        }

        return toGroupResponse(group, familyMemberRepository.findAllByGroupId(groupId));
    }

    private FamilyGroup requireGroup(UUID groupId) {
        return familyGroupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
    }

    private FamilyMember requireMembership(UUID groupId, UUID userId) {
        return familyMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new AccessDeniedException("You do not have access to this group"));
    }

    private void requireManager(FamilyMember member) {
        if (member.getRole() != FamilyRole.OWNER && member.getRole() != FamilyRole.ADMIN) {
            throw new AccessDeniedException("Only owner or admin can manage group members");
        }
    }

    private void requireOwner(FamilyMember member) {
        if (member.getRole() != FamilyRole.OWNER) {
            throw new AccessDeniedException("Only group owner can perform this action");
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

    private GroupResponse toGroupResponse(FamilyGroup group, List<FamilyMember> members) {
        List<MemberResponse> memberResponses = members.stream()
                .sorted(Comparator.comparing(FamilyMember::getJoinedAt))
                .map(this::toMemberResponse)
                .toList();

        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getOwnerUserId(),
                group.getCreatedAt(),
                group.getUpdatedAt(),
                memberResponses
        );
    }

    private MemberResponse toMemberResponse(FamilyMember member) {
        return new MemberResponse(
                member.getId(),
                member.getGroupId(),
                member.getUserId(),
                member.getRole(),
                member.getJoinedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
