package com.house.expensetracker.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.house.expensetracker.dto.HouseholdDto;
import com.house.expensetracker.dto.HouseholdInvitationDto;
import com.house.expensetracker.security.AuthenticatedUser;
import com.house.expensetracker.service.HouseholdInvitationService;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final HouseholdInvitationService invitationService;

    @Autowired
    public InvitationController(HouseholdInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<HouseholdInvitationDto>> getPendingInvitations(Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        List<HouseholdInvitationDto> list = invitationService.getPendingInvitationsForEmail(caller.email());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<HouseholdDto> acceptInvitation(
            @PathVariable("id") String id,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        HouseholdDto accepted = invitationService.acceptInvitation(
                id,
                caller.uid(),
                caller.email(),
                caller.displayName());
        return ResponseEntity.ok(accepted);
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<Map<String, Object>> declineInvitation(
            @PathVariable("id") String id,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        Map<String, Object> result = invitationService.declineInvitation(id, caller.uid(), caller.email());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelInvitation(
            @PathVariable("id") String id,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        invitationService.cancelInvitationById(id, caller.uid());
        return ResponseEntity.ok(Map.of("id", id, "status", "CANCELLED"));
    }
}
