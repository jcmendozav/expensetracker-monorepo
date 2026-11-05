package com.house.expensetracker.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.house.expensetracker.dto.CreateHouseholdRequest;
import com.house.expensetracker.dto.HouseholdDto;
import com.house.expensetracker.dto.HouseholdMemberDto;
import com.house.expensetracker.dto.UpdateHouseholdRequest;
import com.house.expensetracker.exception.BadRequestException;
import com.house.expensetracker.exception.ForbiddenException;
import com.house.expensetracker.exception.ResourceNotFoundException;
import com.house.expensetracker.model.Household;
import com.house.expensetracker.model.HouseholdMember;

@Service
public class HouseholdService {

    public static final String COLLECTION_NAME = "households";

    private final Firestore firestore;

    @Autowired
    public HouseholdService(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference getCollection() {
        return firestore.collection(COLLECTION_NAME);
    }

    public HouseholdDto createHousehold(CreateHouseholdRequest request, String callerUid, String callerEmail, String callerDisplayName) {
        if (callerUid == null || callerUid.isBlank()) {
            throw new BadRequestException("Caller UID is required");
        }

        DocumentReference docRef = getCollection().document();
        Timestamp now = Timestamp.now();

        HouseholdMember ownerMember = HouseholdMember.builder()
                .userId(callerUid)
                .role("PRIMARY_OWNER")
                .displayName(callerDisplayName != null && !callerDisplayName.isBlank() ? callerDisplayName : "Owner")
                .email(callerEmail != null ? callerEmail.toLowerCase().trim() : "")
                .status("ACTIVE")
                .build();

        Map<String, HouseholdMember> membersMap = new HashMap<>();
        membersMap.put(callerUid, ownerMember);

        List<String> memberIds = new ArrayList<>();
        memberIds.add(callerUid);

        Household household = Household.builder()
                .id(docRef.getId())
                .name(request.getName().trim())
                .status("ACTIVE")
                .baseCurrencies(new ArrayList<>(request.getBaseCurrencies()))
                .balancesByCurrency(new HashMap<>())
                .ownerId(callerUid)
                .members(membersMap)
                .memberIds(memberIds)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            docRef.set(toFirestoreMap(household)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while creating household", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to create household: " + e.getMessage(), e);
        }

        return toDto(household);
    }

    public HouseholdDto getCurrentHousehold(String callerUid) {
        if (callerUid == null || callerUid.isBlank()) {
            throw new BadRequestException("Caller UID is required");
        }

        try {
            ApiFuture<QuerySnapshot> future = getCollection()
                    .whereArrayContains("memberIds", callerUid)
                    .get();

            List<QueryDocumentSnapshot> docs = future.get().getDocuments();
            for (QueryDocumentSnapshot doc : docs) {
                Household h = fromSnapshot(doc);
                if (h != null && !"ARCHIVED".equalsIgnoreCase(h.getStatus())) {
                    return toDto(h);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching current household", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to fetch current household: " + e.getMessage(), e);
        }

        throw new ResourceNotFoundException("No active household found for user: " + callerUid);
    }

    public HouseholdDto getHouseholdById(String householdId, String callerUid) {
        Household household = getHouseholdEntity(householdId);
        assertMembership(household, callerUid);
        return toDto(household);
    }

    public HouseholdDto updateHousehold(String householdId, UpdateHouseholdRequest request, String callerUid) {
        Household household = getHouseholdEntity(householdId);
        assertMembership(household, callerUid);

        boolean changed = false;
        if (request.getName() != null && !request.getName().isBlank()) {
            household.setName(request.getName().trim());
            changed = true;
        }

        if (request.getBaseCurrencies() != null && !request.getBaseCurrencies().isEmpty()) {
            household.setBaseCurrencies(new ArrayList<>(request.getBaseCurrencies()));
            changed = true;
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String nextStatus = request.getStatus().toUpperCase().trim();
            if ("ARCHIVED".equals(nextStatus) && !callerUid.equals(household.getOwnerId())) {
                throw new ForbiddenException("Only the Primary Owner can archive or close the household");
            }
            household.setStatus(nextStatus);
            changed = true;
        }

        if (changed) {
            household.setUpdatedAt(Timestamp.now());
            try {
                getCollection().document(householdId).set(toFirestoreMap(household)).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while updating household", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Failed to update household: " + e.getMessage(), e);
            }
        }

        return toDto(household);
    }

    public Household getHouseholdEntity(String householdId) {
        try {
            DocumentSnapshot doc = getCollection().document(householdId).get().get();
            if (!doc.exists()) {
                throw new ResourceNotFoundException("Household not found: " + householdId);
            }
            return fromSnapshot(doc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching household entity", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to fetch household: " + e.getMessage(), e);
        }
    }

    public void assertMembership(Household household, String callerUid) {
        if (household == null || callerUid == null || !household.getMemberIds().contains(callerUid)) {
            throw new ForbiddenException("Caller is not an authorized member of household");
        }
    }

    public HouseholdDto toDto(Household entity) {
        if (entity == null) {
            return null;
        }

        Map<String, HouseholdMemberDto> memberDtoMap = new HashMap<>();
        if (entity.getMembers() != null) {
            entity.getMembers().forEach((uid, m) -> memberDtoMap.put(uid, HouseholdMemberDto.builder()
                    .userId(m.getUserId())
                    .role(m.getRole())
                    .displayName(m.getDisplayName())
                    .email(m.getEmail())
                    .status(m.getStatus())
                    .build()));
        }

        Instant created = entity.getCreatedAt() != null ? Instant.ofEpochSecond(entity.getCreatedAt().getSeconds(), entity.getCreatedAt().getNanos()) : Instant.now();
        Instant updated = entity.getUpdatedAt() != null ? Instant.ofEpochSecond(entity.getUpdatedAt().getSeconds(), entity.getUpdatedAt().getNanos()) : Instant.now();

        return HouseholdDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .baseCurrencies(entity.getBaseCurrencies() != null ? new ArrayList<>(entity.getBaseCurrencies()) : new ArrayList<>())
                .balancesByCurrency(entity.getBalancesByCurrency() != null ? new HashMap<>(entity.getBalancesByCurrency()) : new HashMap<>())
                .ownerId(entity.getOwnerId())
                .members(memberDtoMap)
                .memberIds(entity.getMemberIds() != null ? new ArrayList<>(entity.getMemberIds()) : new ArrayList<>())
                .createdAt(created)
                .updatedAt(updated)
                .build();
    }

    private Map<String, Object> toFirestoreMap(Household h) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", h.getId());
        map.put("name", h.getName());
        map.put("status", h.getStatus());
        map.put("baseCurrencies", h.getBaseCurrencies());
        map.put("balancesByCurrency", h.getBalancesByCurrency());
        map.put("ownerId", h.getOwnerId());
        map.put("memberIds", h.getMemberIds());
        map.put("createdAt", h.getCreatedAt());
        map.put("updatedAt", h.getUpdatedAt());

        Map<String, Object> membersMap = new HashMap<>();
        if (h.getMembers() != null) {
            h.getMembers().forEach((k, v) -> {
                Map<String, Object> m = new HashMap<>();
                m.put("userId", v.getUserId());
                m.put("role", v.getRole());
                m.put("displayName", v.getDisplayName());
                m.put("email", v.getEmail());
                m.put("status", v.getStatus());
                membersMap.put(k, m);
            });
        }
        map.put("members", membersMap);
        return map;
    }

    @SuppressWarnings("unchecked")
    private Household fromSnapshot(DocumentSnapshot doc) {
        if (!doc.exists()) {
            return null;
        }

        Household h = new Household();
        h.setId(doc.getId());
        h.setName(doc.getString("name"));
        h.setStatus(doc.getString("status"));
        h.setOwnerId(doc.getString("ownerId"));
        h.setCreatedAt(doc.getTimestamp("createdAt"));
        h.setUpdatedAt(doc.getTimestamp("updatedAt"));

        List<String> baseCurrencies = (List<String>) doc.get("baseCurrencies");
        h.setBaseCurrencies(baseCurrencies != null ? baseCurrencies : new ArrayList<>());

        List<String> memberIds = (List<String>) doc.get("memberIds");
        h.setMemberIds(memberIds != null ? memberIds : new ArrayList<>());

        Map<String, Object> rawBalances = (Map<String, Object>) doc.get("balancesByCurrency");
        Map<String, Long> balances = new HashMap<>();
        if (rawBalances != null) {
            rawBalances.forEach((k, v) -> {
                if (v instanceof Number) {
                    balances.put(k, ((Number) v).longValue());
                }
            });
        }
        h.setBalancesByCurrency(balances);

        Map<String, Object> rawMembers = (Map<String, Object>) doc.get("members");
        Map<String, HouseholdMember> members = new HashMap<>();
        if (rawMembers != null) {
            rawMembers.forEach((k, v) -> {
                if (v instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) v;
                    members.put(k, HouseholdMember.builder()
                            .userId((String) m.get("userId"))
                            .role((String) m.get("role"))
                            .displayName((String) m.get("displayName"))
                            .email((String) m.get("email"))
                            .status((String) m.get("status"))
                            .build());
                }
            });
        }
        h.setMembers(members);

        return h;
    }
}
