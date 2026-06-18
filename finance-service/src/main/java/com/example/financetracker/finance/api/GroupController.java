package com.example.financetracker.finance.api;

import com.example.financetracker.finance.api.dto.AddMemberRequest;
import com.example.financetracker.finance.api.dto.CreateGroupRequest;
import com.example.financetracker.finance.api.dto.GroupResponse;
import com.example.financetracker.finance.api.dto.MemberResponse;
import com.example.financetracker.finance.service.FamilyGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}
