package com.example.financetracker.finance.api;

import com.example.financetracker.finance.api.dto.AddMemberRequest;
import com.example.financetracker.finance.api.dto.ChangeMemberRoleRequest;
import com.example.financetracker.finance.api.dto.CreateGroupRequest;
import com.example.financetracker.finance.api.dto.GroupResponse;
import com.example.financetracker.finance.api.dto.MemberResponse;
import com.example.financetracker.finance.api.dto.TransferOwnershipRequest;
import com.example.financetracker.finance.api.dto.UpdateGroupRequest;
import com.example.financetracker.finance.service.FamilyGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Family Groups")
public class GroupController {

    private final FamilyGroupService familyGroupService;

    public GroupController(FamilyGroupService familyGroupService) {
        this.familyGroupService = familyGroupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a family group")
    public GroupResponse createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return familyGroupService.createGroup(request);
    }

    @GetMapping
    @Operation(summary = "Get groups for the current user")
    public List<GroupResponse> getMyGroups() {
        return familyGroupService.getMyGroups();
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get a group by id")
    public GroupResponse getGroupById(@PathVariable UUID groupId) {
        return familyGroupService.getGroupById(groupId);
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "Update a group")
    public GroupResponse updateGroup(@PathVariable UUID groupId, @Valid @RequestBody UpdateGroupRequest request) {
        return familyGroupService.updateGroup(groupId, request);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a group")
    public void deleteGroup(@PathVariable UUID groupId) {
        familyGroupService.deleteGroup(groupId);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a member to the group")
    public MemberResponse addMember(@PathVariable UUID groupId, @Valid @RequestBody AddMemberRequest request) {
        return familyGroupService.addMember(groupId, request);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a member from the group")
    public void removeMember(@PathVariable UUID groupId, @PathVariable UUID userId) {
        familyGroupService.removeMember(groupId, userId);
    }

    @PatchMapping("/{groupId}/members/{userId}/role")
    @Operation(summary = "Change a group member role")
    public MemberResponse changeMemberRole(
            @PathVariable UUID groupId,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeMemberRoleRequest request
    ) {
        return familyGroupService.changeMemberRole(groupId, userId, request);
    }

    @PostMapping("/{groupId}/transfer-owner")
    @Operation(summary = "Transfer group ownership to another member")
    public GroupResponse transferOwnership(
            @PathVariable UUID groupId,
            @Valid @RequestBody TransferOwnershipRequest request
    ) {
        return familyGroupService.transferOwnership(groupId, request);
    }
}
