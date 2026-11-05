package com.house.expensetracker.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.house.expensetracker.dto.CreateHouseholdRequest;
import com.house.expensetracker.dto.HouseholdDto;
import com.house.expensetracker.dto.HouseholdInvitationDto;
import com.house.expensetracker.dto.InviteCoOwnerRequest;
import com.house.expensetracker.dto.UpdateHouseholdRequest;
import com.house.expensetracker.security.AuthenticatedUser;
import com.house.expensetracker.service.HouseholdInvitationService;
import com.house.expensetracker.service.HouseholdService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/households")
public class HouseholdController {

    private final HouseholdService householdService;
    private final HouseholdInvitationService invitationService;

    @Autowired
    public HouseholdController(HouseholdService householdService, HouseholdInvitationService invitationService) {
        this.householdService = householdService;
        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<HouseholdDto> createHousehold(
            @Valid @RequestBody CreateHouseholdRequest request,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        HouseholdDto created = householdService.createHousehold(
                request,
                caller.uid(),
                caller.email(),
                caller.displayName());

        // If partner email provided on creation, trigger invitation
        if (request.getPartnerEmail() != null && !request.getPartnerEmail().isBlank()) {
            invitationService.createInvitation(
                    created.getId(),
                    InviteCoOwnerRequest.builder().invitedEmail(request.getPartnerEmail()).build(),
                    caller.uid(),
                    caller.displayName());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/current")
    public ResponseEntity<HouseholdDto> getCurrentHousehold(Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        HouseholdDto current = householdService.getCurrentHousehold(caller.uid());
        return ResponseEntity.ok(current);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HouseholdDto> getHouseholdById(
            @PathVariable("id") String id,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        HouseholdDto household = householdService.getHouseholdById(id, caller.uid());
        return ResponseEntity.ok(household);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<HouseholdDto> updateHousehold(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateHouseholdRequest request,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        HouseholdDto updated = householdService.updateHousehold(id, request, caller.uid());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/invitations")
    public ResponseEntity<HouseholdInvitationDto> inviteCoOwner(
            @PathVariable("id") String id,
            @Valid @RequestBody InviteCoOwnerRequest request,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        HouseholdInvitationDto invitation = invitationService.createInvitation(
                id,
                request,
                caller.uid(),
                caller.displayName());
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
    }

    @GetMapping("/{id}/invitations")
    public ResponseEntity<List<HouseholdInvitationDto>> getHouseholdInvitations(
            @PathVariable("id") String id,
            Principal principal) {
        String callerUid = principal.getName();
        List<HouseholdInvitationDto> list = invitationService.getInvitationsForHousehold(id, callerUid);
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}/invitations/{invitationId}")
    public ResponseEntity<Void> cancelInvitation(
            @PathVariable("id") String householdId,
            @PathVariable("invitationId") String invitationId,
            Principal principal) {
        String callerUid = principal.getName();
        invitationService.cancelInvitation(householdId, invitationId, callerUid);
        return ResponseEntity.noContent().build();
    }
}
