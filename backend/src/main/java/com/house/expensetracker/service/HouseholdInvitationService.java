package com.house.expensetracker.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.house.expensetracker.dto.HouseholdDto;
import com.house.expensetracker.dto.HouseholdInvitationDto;
import com.house.expensetracker.dto.InviteCoOwnerRequest;
import com.house.expensetracker.exception.BadRequestException;
import com.house.expensetracker.exception.ResourceNotFoundException;
import com.house.expensetracker.model.Household;
import com.house.expensetracker.model.HouseholdInvitation;
import com.house.expensetracker.model.HouseholdMember;

@Service
public class HouseholdInvitationService {

    public static final String COLLECTION_NAME = "householdInvitations";

    private final Firestore firestore;
    private final HouseholdService householdService;

    @Autowired
    public HouseholdInvitationService(Firestore firestore, HouseholdService householdService) {
        this.firestore = firestore;
        this.householdService = householdService;
    }

    private CollectionReference getCollection() {
        return firestore.collection(COLLECTION_NAME);
    }

    public HouseholdInvitationDto createInvitation(
            String householdId,
            InviteCoOwnerRequest request,
            String callerUid,
            String callerName) {
        if (callerUid == null || callerUid.isBlank()) {
            throw new BadRequestException("Caller UID is required");
        }

        Household household = householdService.getHouseholdEntity(householdId);
        householdService.assertMembership(household, callerUid);

        String normalizedEmail = request.getInvitedEmail().toLowerCase().trim();

        // Check if user is already an active member
        if (household.getMembers() != null) {
            boolean alreadyMember = household.getMembers().values().stream()
                    .anyMatch(m -> normalizedEmail.equalsIgnoreCase(m.getEmail()));
            if (alreadyMember) {
                throw new BadRequestException("User with email " + normalizedEmail + " is already a household member");
            }
        }

        DocumentReference docRef = getCollection().document();
        Timestamp now = Timestamp.now();

        HouseholdInvitation invitation = HouseholdInvitation.builder()
                .id(docRef.getId())
                .householdId(household.getId())
                .householdName(household.getName())
                .inviterId(callerUid)
                .inviterName(callerName != null && !callerName.isBlank() ? callerName : "Primary Owner")
                .invitedEmail(normalizedEmail)
                .role("CO_OWNER")
                .status("PENDING")
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            docRef.set(toFirestoreMap(invitation)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while creating invitation", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to create invitation: " + e.getMessage(), e);
        }

        return toDto(invitation);
    }

    public List<HouseholdInvitationDto> getPendingInvitationsForEmail(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }

        String normalizedEmail = email.toLowerCase().trim();
        List<HouseholdInvitationDto> list = new ArrayList<>();

        try {
            QuerySnapshot snapshot = getCollection()
                    .whereEqualTo("invitedEmail", normalizedEmail)
                    .whereEqualTo("status", "PENDING")
                    .get().get();

            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                list.add(toDto(fromSnapshot(doc)));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching pending invitations", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to fetch pending invitations: " + e.getMessage(), e);
        }

        return list;
    }

    public List<HouseholdInvitationDto> getInvitationsForHousehold(String householdId, String callerUid) {
        Household household = householdService.getHouseholdEntity(householdId);
        householdService.assertMembership(household, callerUid);

        List<HouseholdInvitationDto> list = new ArrayList<>();
        try {
            QuerySnapshot snapshot = getCollection()
                    .whereEqualTo("householdId", householdId)
                    .get().get();

            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                list.add(toDto(fromSnapshot(doc)));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching household invitations", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to fetch household invitations: " + e.getMessage(), e);
        }

        return list;
    }

    public HouseholdDto acceptInvitation(
            String invitationId,
            String callerUid,
            String callerEmail,
            String callerName) {
        if (callerUid == null || callerUid.isBlank()) {
            throw new BadRequestException("Caller UID is required");
        }

        HouseholdInvitation invitation = getInvitationEntity(invitationId);
        if (!"PENDING".equalsIgnoreCase(invitation.getStatus())) {
            throw new BadRequestException("Invitation is no longer pending (current status: " + invitation.getStatus() + ")");
        }

        Timestamp now = Timestamp.now();
        invitation.setStatus("ACCEPTED");
        invitation.setUpdatedAt(now);

        try {
            // 1. Mark invitation accepted
            getCollection().document(invitationId).update("status", "ACCEPTED", "updatedAt", now).get();

            // 2. Add caller to household members
            Household household = householdService.getHouseholdEntity(invitation.getHouseholdId());
            List<String> memberIds = household.getMemberIds() != null
                    ? new ArrayList<>(household.getMemberIds())
                    : new ArrayList<>();
            if (!memberIds.contains(callerUid)) {
                memberIds.add(callerUid);
            }
            household.setMemberIds(memberIds);

            Map<String, HouseholdMember> members = household.getMembers() != null
                    ? new HashMap<>(household.getMembers())
                    : new HashMap<>();

            HouseholdMember newMember = HouseholdMember.builder()
                    .userId(callerUid)
                    .role("CO_OWNER")
                    .displayName(callerName != null && !callerName.isBlank() ? callerName : "Co-Owner")
                    .email(callerEmail != null ? callerEmail.toLowerCase().trim() : invitation.getInvitedEmail())
                    .status("ACTIVE")
                    .build();
            members.put(callerUid, newMember);
            household.setMembers(members);
            household.setUpdatedAt(now);

            // Update in Firestore
            DocumentReference hhDocRef = firestore.collection(HouseholdService.COLLECTION_NAME).document(household.getId());
            Map<String, Object> membersMap = new HashMap<>();
            members.forEach((k, v) -> {
                Map<String, Object> m = new HashMap<>();
                m.put("userId", v.getUserId());
                m.put("role", v.getRole());
                m.put("displayName", v.getDisplayName());
                m.put("email", v.getEmail());
                m.put("status", v.getStatus());
                membersMap.put(k, m);
            });

            hhDocRef.update("memberIds", memberIds, "members", membersMap, "updatedAt", now).get();

            return householdService.toDto(household);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while accepting invitation", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to accept invitation: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> declineInvitation(String invitationId, String callerUid) {
        HouseholdInvitation invitation = getInvitationEntity(invitationId);
        if (!"PENDING".equalsIgnoreCase(invitation.getStatus())) {
            throw new BadRequestException("Invitation is not pending");
        }

        Timestamp now = Timestamp.now();
        invitation.setStatus("DECLINED");
        invitation.setUpdatedAt(now);

        try {
            getCollection().document(invitationId).update("status", "DECLINED", "updatedAt", now).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while declining invitation", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to decline invitation: " + e.getMessage(), e);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("id", invitationId);
        res.put("status", "DECLINED");
        return res;
    }

    public void cancelInvitation(String householdId, String invitationId, String callerUid) {
        Household household = householdService.getHouseholdEntity(householdId);
        householdService.assertMembership(household, callerUid);

        HouseholdInvitation invitation = getInvitationEntity(invitationId);
        if (!householdId.equals(invitation.getHouseholdId())) {
            throw new BadRequestException("Invitation does not belong to specified household");
        }

        Timestamp now = Timestamp.now();
        try {
            getCollection().document(invitationId).update("status", "CANCELLED", "updatedAt", now).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while cancelling invitation", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to cancel invitation: " + e.getMessage(), e);
        }
    }

    public HouseholdInvitation getInvitationEntity(String invitationId) {
        try {
            DocumentSnapshot doc = getCollection().document(invitationId).get().get();
            if (!doc.exists()) {
                throw new ResourceNotFoundException("Invitation not found: " + invitationId);
            }
            return fromSnapshot(doc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching invitation", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to fetch invitation: " + e.getMessage(), e);
        }
    }

    public HouseholdInvitationDto toDto(HouseholdInvitation inv) {
        if (inv == null) {
            return null;
        }
        Instant created = inv.getCreatedAt() != null
                ? Instant.ofEpochSecond(inv.getCreatedAt().getSeconds(), inv.getCreatedAt().getNanos())
                : Instant.now();
        Instant updated = inv.getUpdatedAt() != null
                ? Instant.ofEpochSecond(inv.getUpdatedAt().getSeconds(), inv.getUpdatedAt().getNanos())
                : Instant.now();

        return HouseholdInvitationDto.builder()
                .id(inv.getId())
                .householdId(inv.getHouseholdId())
                .householdName(inv.getHouseholdName())
                .inviterId(inv.getInviterId())
                .inviterName(inv.getInviterName())
                .invitedEmail(inv.getInvitedEmail())
                .role(inv.getRole())
                .status(inv.getStatus())
                .createdAt(created)
                .updatedAt(updated)
                .build();
    }

    private Map<String, Object> toFirestoreMap(HouseholdInvitation inv) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", inv.getId());
        map.put("householdId", inv.getHouseholdId());
        map.put("householdName", inv.getHouseholdName());
        map.put("inviterId", inv.getInviterId());
        map.put("inviterName", inv.getInviterName());
        map.put("invitedEmail", inv.getInvitedEmail());
        map.put("role", inv.getRole());
        map.put("status", inv.getStatus());
        map.put("createdAt", inv.getCreatedAt());
        map.put("updatedAt", inv.getUpdatedAt());
        return map;
    }

    private HouseholdInvitation fromSnapshot(DocumentSnapshot doc) {
        HouseholdInvitation inv = new HouseholdInvitation();
        inv.setId(doc.getId());
        inv.setHouseholdId(doc.getString("householdId"));
        inv.setHouseholdName(doc.getString("householdName"));
        inv.setInviterId(doc.getString("inviterId"));
        inv.setInviterName(doc.getString("inviterName"));
        inv.setInvitedEmail(doc.getString("invitedEmail"));
        inv.setRole(doc.getString("role"));
        inv.setStatus(doc.getString("status"));
        inv.setCreatedAt(doc.getTimestamp("createdAt"));
        inv.setUpdatedAt(doc.getTimestamp("updatedAt"));
        return inv;
    }
}

