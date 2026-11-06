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
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.house.expensetracker.dto.BudgetDto;
import com.house.expensetracker.dto.BudgetListResponse;
import com.house.expensetracker.dto.CreateBudgetRequest;
import com.house.expensetracker.dto.LiquiditySummaryResponse;
import com.house.expensetracker.dto.UpdateBudgetRequest;
import com.house.expensetracker.exception.BadRequestException;
import com.house.expensetracker.exception.ResourceNotFoundException;
import com.house.expensetracker.model.Budget;
import com.house.expensetracker.model.Household;

@Service
public class BudgetService {

    public static final String COLLECTION_NAME = "budgets";

    private final Firestore firestore;
    private final HouseholdService householdService;

    @Autowired
    public BudgetService(Firestore firestore, HouseholdService householdService) {
        this.firestore = firestore;
        this.householdService = householdService;
    }

    private CollectionReference getCollection() {
        return firestore.collection(COLLECTION_NAME);
    }

    public BudgetDto createBudget(CreateBudgetRequest request, String callerUid) {
        if (callerUid == null || callerUid.isBlank()) {
            throw new BadRequestException("Caller UID is required");
        }

        Household household = householdService.getHouseholdEntity(request.getHouseholdId());
        householdService.assertMembership(household, callerUid);

        DocumentReference budgetDocRef = getCollection().document();
        Timestamp now = Timestamp.now();
        String currency = request.getCurrency().toUpperCase().trim();

        Budget budget = Budget.builder()
                .id(budgetDocRef.getId())
                .householdId(household.getId())
                .title(request.getTitle().trim())
                .initialAmountCents(request.getInitialAmountCents())
                .currentBalanceCents(request.getInitialAmountCents())
                .currency(currency)
                .status("ACTIVE")
                .ownerId(callerUid)
                .createdAt(now)
                .updatedAt(now)
                .build();

        DocumentReference hhDocRef = firestore.collection(HouseholdService.COLLECTION_NAME).document(household.getId());
        Map<String, Long> balances = household.getBalancesByCurrency() != null
                ? new HashMap<>(household.getBalancesByCurrency())
                : new HashMap<>();
        balances.put(currency, balances.getOrDefault(currency, 0L) + request.getInitialAmountCents());
        int nextActiveCount = (household.getActiveBudgetCount() != null ? household.getActiveBudgetCount() : 0) + 1;

        household.setBalancesByCurrency(balances);
        household.setActiveBudgetCount(nextActiveCount);
        household.setUpdatedAt(now);

        try {
            WriteBatch batch = firestore.batch();
            batch.set(budgetDocRef, toFirestoreMap(budget));
            batch.update(hhDocRef,
                    "balancesByCurrency", balances,
                    "activeBudgetCount", nextActiveCount,
                    "updatedAt", now);
            batch.commit().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while creating budget", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to create budget: " + e.getMessage(), e);
        }

        return toDto(budget);
    }

    public BudgetListResponse listBudgets(String householdId, String statusFilter, String callerUid) {
        if (householdId == null || householdId.isBlank()) {
            throw new BadRequestException("Household ID is required");
        }

        Household household = householdService.getHouseholdEntity(householdId);
        householdService.assertMembership(household, callerUid);

        List<BudgetDto> items = new ArrayList<>();
        try {
            Query query = getCollection().whereEqualTo("householdId", householdId);
            if (statusFilter != null && !statusFilter.isBlank()) {
                query = query.whereEqualTo("status", statusFilter.toUpperCase());
            }

            QuerySnapshot snapshot = query.get().get();
            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                Budget b = fromSnapshot(doc);
                if (statusFilter == null && "ARCHIVED".equalsIgnoreCase(b.getStatus())) {
                    // By default, hide ARCHIVED budgets from active list unless specifically requested
                    continue;
                }
                items.add(toDto(b));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while listing budgets", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to list budgets: " + e.getMessage(), e);
        }

        return BudgetListResponse.builder()
                .items(items)
                .nextCursor(null)
                .build();
    }

    public BudgetDto getBudgetById(String budgetId, String callerUid) {
        Budget budget = getBudgetEntity(budgetId);
        Household household = householdService.getHouseholdEntity(budget.getHouseholdId());
        householdService.assertMembership(household, callerUid);
        return toDto(budget);
    }

    public BudgetDto updateBudget(String budgetId, UpdateBudgetRequest request, String callerUid) {
        Budget budget = getBudgetEntity(budgetId);
        Household household = householdService.getHouseholdEntity(budget.getHouseholdId());
        householdService.assertMembership(household, callerUid);

        String oldStatus = budget.getStatus();
        long oldBalance = budget.getCurrentBalanceCents() != null ? budget.getCurrentBalanceCents() : 0L;
        long oldInitial = budget.getInitialAmountCents() != null ? budget.getInitialAmountCents() : 0L;
        boolean oldWasArchived = "ARCHIVED".equalsIgnoreCase(oldStatus);
        boolean oldWasActive = Budget.isActiveStatus(oldStatus);

        boolean changed = false;
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            budget.setTitle(request.getTitle().trim());
            changed = true;
        }

        if (request.getInitialAmountCents() != null && request.getInitialAmountCents() > 0
                && !request.getInitialAmountCents().equals(oldInitial)) {
            long allocationDelta = request.getInitialAmountCents() - oldInitial;
            long updatedBalance = oldBalance + allocationDelta;
            budget.setInitialAmountCents(request.getInitialAmountCents());
            budget.setCurrentBalanceCents(updatedBalance);
            budget.setStatus(Budget.deriveStatus(updatedBalance, request.getInitialAmountCents(), budget.getStatus()));
            changed = true;
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String requestedStatus = request.getStatus().toUpperCase().trim();
            if ("ARCHIVED".equals(requestedStatus)) {
                budget.setStatus("ARCHIVED");
            } else {
                budget.setStatus(Budget.deriveStatus(
                        budget.getCurrentBalanceCents(),
                        budget.getInitialAmountCents(),
                        requestedStatus));
            }
            changed = true;
        }

        if (changed) {
            Timestamp now = Timestamp.now();
            budget.setUpdatedAt(now);

            boolean newIsArchived = "ARCHIVED".equalsIgnoreCase(budget.getStatus());
            boolean newIsActive = Budget.isActiveStatus(budget.getStatus());
            long oldContribution = oldWasArchived ? 0L : oldBalance;
            long newContribution = newIsArchived ? 0L : budget.getCurrentBalanceCents();
            long hhBalanceDelta = newContribution - oldContribution;
            int activeCountDelta = (newIsActive ? 1 : 0) - (oldWasActive ? 1 : 0);

            try {
                WriteBatch batch = firestore.batch();
                DocumentReference budgetDocRef = getCollection().document(budgetId);
                batch.set(budgetDocRef, toFirestoreMap(budget));

                if (hhBalanceDelta != 0 || activeCountDelta != 0) {
                    DocumentReference hhDocRef = firestore
                            .collection(HouseholdService.COLLECTION_NAME)
                            .document(household.getId());
                    Map<String, Long> balances = household.getBalancesByCurrency() != null
                            ? new HashMap<>(household.getBalancesByCurrency())
                            : new HashMap<>();
                    if (hhBalanceDelta != 0) {
                        balances.put(
                                budget.getCurrency(),
                                balances.getOrDefault(budget.getCurrency(), 0L) + hhBalanceDelta);
                    }
                    int currentActive = household.getActiveBudgetCount() != null ? household.getActiveBudgetCount() : 0;
                    int nextActive = Math.max(0, currentActive + activeCountDelta);
                    household.setBalancesByCurrency(balances);
                    household.setActiveBudgetCount(nextActive);
                    household.setUpdatedAt(now);

                    batch.update(hhDocRef,
                            "balancesByCurrency", balances,
                            "activeBudgetCount", nextActive,
                            "updatedAt", now);
                }

                batch.commit().get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while updating budget", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Failed to update budget: " + e.getMessage(), e);
            }
        }

        return toDto(budget);
    }

    /**
     * Reads strictly 1 cached Household document per §3.6 (Pre-Calculated Household Liquidity Snapshot)
     * rather than querying all N budget documents.
     */
    public LiquiditySummaryResponse getLiquiditySummary(String householdId, String callerUid) {
        if (householdId == null || householdId.isBlank()) {
            throw new BadRequestException("Household ID is required");
        }

        Household household = householdService.getHouseholdEntity(householdId);
        householdService.assertMembership(household, callerUid);

        return LiquiditySummaryResponse.builder()
                .balancesByCurrency(household.getBalancesByCurrency() != null
                        ? new HashMap<>(household.getBalancesByCurrency())
                        : Map.of())
                .activeCount(household.getActiveBudgetCount() != null ? household.getActiveBudgetCount() : 0)
                .build();
    }

    public Budget getBudgetEntity(String budgetId) {
        try {
            DocumentSnapshot doc = getCollection().document(budgetId).get().get();
            if (!doc.exists()) {
                throw new ResourceNotFoundException("Budget not found: " + budgetId);
            }
            return fromSnapshot(doc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching budget", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to fetch budget: " + e.getMessage(), e);
        }
    }

    public BudgetDto toDto(Budget b) {
        if (b == null) {
            return null;
        }
        Instant created = b.getCreatedAt() != null
                ? Instant.ofEpochSecond(b.getCreatedAt().getSeconds(), b.getCreatedAt().getNanos())
                : Instant.now();
        Instant updated = b.getUpdatedAt() != null
                ? Instant.ofEpochSecond(b.getUpdatedAt().getSeconds(), b.getUpdatedAt().getNanos())
                : Instant.now();

        return BudgetDto.builder()
                .id(b.getId())
                .householdId(b.getHouseholdId())
                .title(b.getTitle())
                .initialAmountCents(b.getInitialAmountCents())
                .currentBalanceCents(b.getCurrentBalanceCents())
                .currency(b.getCurrency())
                .status(b.getStatus())
                .ownerId(b.getOwnerId())
                .createdAt(created)
                .updatedAt(updated)
                .build();
    }

    public Map<String, Object> toFirestoreMap(Budget b) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", b.getId());
        map.put("householdId", b.getHouseholdId());
        map.put("title", b.getTitle());
        map.put("initialAmountCents", b.getInitialAmountCents());
        map.put("currentBalanceCents", b.getCurrentBalanceCents());
        map.put("currency", b.getCurrency());
        map.put("status", b.getStatus());
        map.put("ownerId", b.getOwnerId());
        map.put("createdAt", b.getCreatedAt());
        map.put("updatedAt", b.getUpdatedAt());
        return map;
    }

    public Budget fromSnapshot(DocumentSnapshot doc) {
        Budget b = new Budget();
        b.setId(doc.getId());
        b.setHouseholdId(doc.getString("householdId"));
        b.setTitle(doc.getString("title"));
        b.setCurrency(doc.getString("currency"));
        b.setStatus(doc.getString("status"));
        b.setOwnerId(doc.getString("ownerId"));

        Long initialAmount = doc.getLong("initialAmountCents");
        b.setInitialAmountCents(initialAmount != null ? initialAmount : 0L);

        Long currentBalance = doc.getLong("currentBalanceCents");
        b.setCurrentBalanceCents(currentBalance != null ? currentBalance : 0L);

        b.setCreatedAt(doc.getTimestamp("createdAt"));
        b.setUpdatedAt(doc.getTimestamp("updatedAt"));
        return b;
    }
}
