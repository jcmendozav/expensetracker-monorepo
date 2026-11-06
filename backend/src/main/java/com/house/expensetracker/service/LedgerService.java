package com.house.expensetracker.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.house.expensetracker.dto.LogTransactionRequest;
import com.house.expensetracker.dto.TransactionDto;
import com.house.expensetracker.dto.TransferRequest;
import com.house.expensetracker.dto.TransferResultDto;
import com.house.expensetracker.dto.UpdateTransactionMetadataRequest;
import com.house.expensetracker.event.BudgetLowBalanceEvent;
import com.house.expensetracker.event.TransactionCreatedEvent;
import com.house.expensetracker.exception.BadRequestException;
import com.house.expensetracker.exception.ResourceNotFoundException;
import com.house.expensetracker.model.Budget;
import com.house.expensetracker.model.Household;
import com.house.expensetracker.model.LedgerTransaction;

@Service
public class LedgerService {

    public static final String TRANSACTIONS_SUBCOLLECTION = "transactions";

    private final Firestore firestore;
    private final BudgetService budgetService;
    private final HouseholdService householdService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public LedgerService(
            Firestore firestore,
            BudgetService budgetService,
            HouseholdService householdService,
            ApplicationEventPublisher eventPublisher) {
        this.firestore = firestore;
        this.budgetService = budgetService;
        this.householdService = householdService;
        this.eventPublisher = eventPublisher;
    }

    private CollectionReference getTransactionCollection(String budgetId) {
        return firestore.collection(BudgetService.COLLECTION_NAME)
                .document(budgetId)
                .collection(TRANSACTIONS_SUBCOLLECTION);
    }

    public TransactionDto commitTransaction(String budgetId, LogTransactionRequest request, String callerUid, String callerName) {
        if (callerUid == null || callerUid.isBlank()) {
            throw new BadRequestException("Caller UID is required");
        }

        Budget budget = budgetService.getBudgetEntity(budgetId);
        Household household = householdService.getHouseholdEntity(budget.getHouseholdId());
        householdService.assertMembership(household, callerUid);

        String type = request.getType().toUpperCase().trim();
        if (!"SPENDING".equals(type) && !"INCOME".equals(type)) {
            throw new BadRequestException("Invalid transaction type: " + type + ". Must be SPENDING or INCOME.");
        }

        long amount = request.getAmountCents();
        long newBalance = "SPENDING".equals(type)
                ? budget.getCurrentBalanceCents() - amount
                : budget.getCurrentBalanceCents() + amount;

        String newBudgetStatus = Budget.deriveStatus(newBalance, budget.getInitialAmountCents(), budget.getStatus());
        Timestamp now = Timestamp.now();
        Timestamp txDate = request.getTransactionDate() != null
                ? Timestamp.ofTimeSecondsAndNanos(request.getTransactionDate().getEpochSecond(), request.getTransactionDate().getNano())
                : now;

        DocumentReference txDocRef = getTransactionCollection(budgetId).document();

        LedgerTransaction tx = LedgerTransaction.builder()
                .id(txDocRef.getId())
                .householdId(household.getId())
                .budgetId(budget.getId())
                .type(type)
                .status("COMMITTED")
                .amountCents(amount)
                .balanceAfterCents(newBalance)
                .category(request.getCategory().trim())
                .note(request.getNote() != null ? request.getNote().trim() : null)
                .transactionDate(txDate)
                .createdBy(callerUid)
                .createdByName(callerName != null && !callerName.isBlank() ? callerName : "User")
                .createdAt(now)
                .build();

        try {
            // 1. Save Transaction Doc
            txDocRef.set(toFirestoreMap(tx)).get();

            // 2. Update Budget Balance & Status
            DocumentReference budgetDocRef = firestore.collection(BudgetService.COLLECTION_NAME).document(budgetId);
            budget.setCurrentBalanceCents(newBalance);
            budget.setStatus(newBudgetStatus);
            budget.setUpdatedAt(now);
            budgetDocRef.update("currentBalanceCents", newBalance, "status", newBudgetStatus, "updatedAt", now).get();

            // 3. Update Household Consolidated Liquidity Snapshot
            DocumentReference hhDocRef = firestore.collection(HouseholdService.COLLECTION_NAME).document(household.getId());
            Map<String, Long> balances = household.getBalancesByCurrency() != null
                    ? new HashMap<>(household.getBalancesByCurrency())
                    : new HashMap<>();
            long delta = "SPENDING".equals(type) ? -amount : amount;
            balances.put(budget.getCurrency(), balances.getOrDefault(budget.getCurrency(), 0L) + delta);
            household.setBalancesByCurrency(balances);
            household.setUpdatedAt(now);
            hhDocRef.update("balancesByCurrency", balances, "updatedAt", now).get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while committing ledger transaction", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to commit ledger transaction: " + e.getMessage(), e);
        }

        TransactionDto dto = toDto(tx);

        // 4. Non-blocking Event Publishing
        eventPublisher.publishEvent(TransactionCreatedEvent.builder()
                .transaction(dto)
                .budgetTitle(budget.getTitle())
                .currentBalanceCents(newBalance)
                .initialAmountCents(budget.getInitialAmountCents())
                .budgetStatus(newBudgetStatus)
                .build());

        if ("LOW_BALANCE".equals(newBudgetStatus)) {
            eventPublisher.publishEvent(BudgetLowBalanceEvent.builder()
                    .householdId(household.getId())
                    .budgetId(budget.getId())
                    .budgetTitle(budget.getTitle())
                    .currentBalanceCents(newBalance)
                    .initialAmountCents(budget.getInitialAmountCents())
                    .actorId(callerUid)
                    .build());
        }

        return dto;
    }

    public List<TransactionDto> listTransactions(String budgetId, int limit, String callerUid) {
        Budget budget = budgetService.getBudgetEntity(budgetId);
        Household household = householdService.getHouseholdEntity(budget.getHouseholdId());
        householdService.assertMembership(household, callerUid);

        int queryLimit = limit > 0 ? limit : 50;
        List<TransactionDto> list = new ArrayList<>();

        try {
            QuerySnapshot snapshot = getTransactionCollection(budgetId)
                    .orderBy("transactionDate", Query.Direction.DESCENDING)
                    .limit(queryLimit)
                    .get().get();

            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                list.add(toDto(fromSnapshot(doc)));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while listing transactions", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to list transactions: " + e.getMessage(), e);
        }

        return list;
    }

    public TransactionDto updateMetadata(String budgetId, String txId, UpdateTransactionMetadataRequest request, String callerUid) {
        Budget budget = budgetService.getBudgetEntity(budgetId);
        Household household = householdService.getHouseholdEntity(budget.getHouseholdId());
        householdService.assertMembership(household, callerUid);

        DocumentReference txRef = getTransactionCollection(budgetId).document(txId);
        LedgerTransaction tx = getTransactionEntity(budgetId, txId);

        boolean changed = false;
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            tx.setCategory(request.getCategory().trim());
            changed = true;
        }
        if (request.getNote() != null) {
            tx.setNote(request.getNote().trim());
            changed = true;
        }

        if (changed) {
            try {
                txRef.update("category", tx.getCategory(), "note", tx.getNote()).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while updating transaction metadata", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Failed to update transaction metadata: " + e.getMessage(), e);
            }
        }

        return toDto(tx);
    }

    public TransactionDto voidTransaction(String budgetId, String txId, String callerUid) {
        Budget budget = budgetService.getBudgetEntity(budgetId);
        Household household = householdService.getHouseholdEntity(budget.getHouseholdId());
        householdService.assertMembership(household, callerUid);

        DocumentReference txRef = getTransactionCollection(budgetId).document(txId);
        LedgerTransaction tx = getTransactionEntity(budgetId, txId);

        // Idempotency check: if already VOIDED, return immediately without duplicate refund
        if ("VOIDED".equalsIgnoreCase(tx.getStatus())) {
            return toDto(tx);
        }

        long amount = tx.getAmountCents();
        long newBalance;
        long hhDelta;

        if ("SPENDING".equalsIgnoreCase(tx.getType())) {
            newBalance = budget.getCurrentBalanceCents() + amount;
            hhDelta = amount;
        } else if ("INCOME".equalsIgnoreCase(tx.getType())) {
            newBalance = budget.getCurrentBalanceCents() - amount;
            hhDelta = -amount;
        } else {
            // For adjustments or transfers, keep balance intact or handle accordingly
            newBalance = budget.getCurrentBalanceCents();
            hhDelta = 0;
        }

        String newBudgetStatus = Budget.deriveStatus(newBalance, budget.getInitialAmountCents(), budget.getStatus());
        Timestamp now = Timestamp.now();

        tx.setStatus("VOIDED");

        try {
            // Update transaction to VOIDED
            txRef.update("status", "VOIDED").get();

            // Refund budget
            DocumentReference budgetDocRef = firestore.collection(BudgetService.COLLECTION_NAME).document(budgetId);
            budget.setCurrentBalanceCents(newBalance);
            budget.setStatus(newBudgetStatus);
            budget.setUpdatedAt(now);
            budgetDocRef.update("currentBalanceCents", newBalance, "status", newBudgetStatus, "updatedAt", now).get();

            // Refund household snapshot
            if (hhDelta != 0) {
                DocumentReference hhDocRef = firestore.collection(HouseholdService.COLLECTION_NAME).document(household.getId());
                Map<String, Long> balances = household.getBalancesByCurrency() != null
                        ? new HashMap<>(household.getBalancesByCurrency())
                        : new HashMap<>();
                balances.put(budget.getCurrency(), balances.getOrDefault(budget.getCurrency(), 0L) + hhDelta);
                household.setBalancesByCurrency(balances);
                household.setUpdatedAt(now);
                hhDocRef.update("balancesByCurrency", balances, "updatedAt", now).get();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while voiding transaction", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to void transaction: " + e.getMessage(), e);
        }

        return toDto(tx);
    }

    public TransferResultDto transferFunds(TransferRequest request, String callerUid, String callerName) {
        if (callerUid == null || callerUid.isBlank()) {
            throw new BadRequestException("Caller UID is required");
        }

        Budget sourceBudget = budgetService.getBudgetEntity(request.getSourceBudgetId());
        Budget targetBudget = budgetService.getBudgetEntity(request.getTargetBudgetId());

        if (sourceBudget.getId().equals(targetBudget.getId())) {
            throw new BadRequestException("Source and target budgets must be different");
        }

        Household household = householdService.getHouseholdEntity(sourceBudget.getHouseholdId());
        householdService.assertMembership(household, callerUid);

        // Ensure target belongs to same household (or member has access)
        if (!household.getId().equals(targetBudget.getHouseholdId())) {
            Household targetHousehold = householdService.getHouseholdEntity(targetBudget.getHouseholdId());
            householdService.assertMembership(targetHousehold, callerUid);
        }

        long sourceAmount = request.getSourceAmountCents();
        long targetAmount;
        Double exchangeRate = request.getExchangeRate();

        if (sourceBudget.getCurrency().equalsIgnoreCase(targetBudget.getCurrency())) {
            targetAmount = sourceAmount;
        } else {
            if (exchangeRate == null || exchangeRate <= 0.0) {
                throw new BadRequestException("Exchange rate is required for cross-currency transfers ("
                        + sourceBudget.getCurrency() + " -> " + targetBudget.getCurrency() + ")");
            }
            targetAmount = Math.round(sourceAmount * exchangeRate);
        }

        long sourceNewBalance = sourceBudget.getCurrentBalanceCents() - sourceAmount;
        long targetNewBalance = targetBudget.getCurrentBalanceCents() + targetAmount;

        String sourceNewStatus = Budget.deriveStatus(sourceNewBalance, sourceBudget.getInitialAmountCents(), sourceBudget.getStatus());
        String targetNewStatus = Budget.deriveStatus(targetNewBalance, targetBudget.getInitialAmountCents(), targetBudget.getStatus());

        String transferGroupId = UUID.randomUUID().toString();
        Timestamp now = Timestamp.now();
        String note = request.getNote() != null ? request.getNote().trim() : "Transfer between budgets";

        DocumentReference sourceTxRef = getTransactionCollection(sourceBudget.getId()).document();
        DocumentReference targetTxRef = getTransactionCollection(targetBudget.getId()).document();

        LedgerTransaction sourceTx = LedgerTransaction.builder()
                .id(sourceTxRef.getId())
                .householdId(sourceBudget.getHouseholdId())
                .budgetId(sourceBudget.getId())
                .type("TRANSFER_OUT")
                .status("COMMITTED")
                .amountCents(sourceAmount)
                .balanceAfterCents(sourceNewBalance)
                .category("Transfer")
                .note(note)
                .linkedBudgetId(targetBudget.getId())
                .transferGroupId(transferGroupId)
                .exchangeRate(exchangeRate)
                .transactionDate(now)
                .createdBy(callerUid)
                .createdByName(callerName != null ? callerName : "User")
                .createdAt(now)
                .build();

        LedgerTransaction targetTx = LedgerTransaction.builder()
                .id(targetTxRef.getId())
                .householdId(targetBudget.getHouseholdId())
                .budgetId(targetBudget.getId())
                .type("TRANSFER_IN")
                .status("COMMITTED")
                .amountCents(targetAmount)
                .balanceAfterCents(targetNewBalance)
                .category("Transfer")
                .note(note)
                .linkedBudgetId(sourceBudget.getId())
                .transferGroupId(transferGroupId)
                .exchangeRate(exchangeRate)
                .transactionDate(now)
                .createdBy(callerUid)
                .createdByName(callerName != null ? callerName : "User")
                .createdAt(now)
                .build();

        try {
            // 1. Save both transactions
            sourceTxRef.set(toFirestoreMap(sourceTx)).get();
            targetTxRef.set(toFirestoreMap(targetTx)).get();

            // 2. Update source and target budgets
            DocumentReference sourceBudgetRef = firestore.collection(BudgetService.COLLECTION_NAME).document(sourceBudget.getId());
            sourceBudget.setCurrentBalanceCents(sourceNewBalance);
            sourceBudget.setStatus(sourceNewStatus);
            sourceBudget.setUpdatedAt(now);
            sourceBudgetRef.update("currentBalanceCents", sourceNewBalance, "status", sourceNewStatus, "updatedAt", now).get();

            DocumentReference targetBudgetRef = firestore.collection(BudgetService.COLLECTION_NAME).document(targetBudget.getId());
            targetBudget.setCurrentBalanceCents(targetNewBalance);
            targetBudget.setStatus(targetNewStatus);
            targetBudget.setUpdatedAt(now);
            targetBudgetRef.update("currentBalanceCents", targetNewBalance, "status", targetNewStatus, "updatedAt", now).get();

            // 3. Update Household liquidity snapshot
            DocumentReference hhDocRef = firestore.collection(HouseholdService.COLLECTION_NAME).document(household.getId());
            Map<String, Long> balances = household.getBalancesByCurrency() != null
                    ? new HashMap<>(household.getBalancesByCurrency())
                    : new HashMap<>();

            balances.put(sourceBudget.getCurrency(), balances.getOrDefault(sourceBudget.getCurrency(), 0L) - sourceAmount);
            balances.put(targetBudget.getCurrency(), balances.getOrDefault(targetBudget.getCurrency(), 0L) + targetAmount);
            household.setBalancesByCurrency(balances);
            household.setUpdatedAt(now);
            hhDocRef.update("balancesByCurrency", balances, "updatedAt", now).get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while executing transfer", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to execute transfer: " + e.getMessage(), e);
        }

        return TransferResultDto.builder()
                .transferGroupId(transferGroupId)
                .sourceTransaction(toDto(sourceTx))
                .targetTransaction(toDto(targetTx))
                .sourceRemainingBalanceCents(sourceNewBalance)
                .targetRemainingBalanceCents(targetNewBalance)
                .build();
    }

    public LedgerTransaction getTransactionEntity(String budgetId, String txId) {
        try {
            DocumentSnapshot doc = getTransactionCollection(budgetId).document(txId).get().get();
            if (!doc.exists()) {
                throw new ResourceNotFoundException("Transaction not found: " + txId);
            }
            return fromSnapshot(doc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching transaction", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to fetch transaction: " + e.getMessage(), e);
        }
    }

    public TransactionDto toDto(LedgerTransaction tx) {
        if (tx == null) {
            return null;
        }
        Instant txDate = tx.getTransactionDate() != null
                ? Instant.ofEpochSecond(tx.getTransactionDate().getSeconds(), tx.getTransactionDate().getNanos())
                : Instant.now();
        Instant createdAt = tx.getCreatedAt() != null
                ? Instant.ofEpochSecond(tx.getCreatedAt().getSeconds(), tx.getCreatedAt().getNanos())
                : Instant.now();

        return TransactionDto.builder()
                .id(tx.getId())
                .householdId(tx.getHouseholdId())
                .budgetId(tx.getBudgetId())
                .idempotencyKey(tx.getIdempotencyKey())
                .type(tx.getType())
                .status(tx.getStatus())
                .amountCents(tx.getAmountCents())
                .balanceAfterCents(tx.getBalanceAfterCents())
                .category(tx.getCategory())
                .note(tx.getNote())
                .linkedBudgetId(tx.getLinkedBudgetId())
                .transferGroupId(tx.getTransferGroupId())
                .exchangeRate(tx.getExchangeRate())
                .transactionDate(txDate)
                .createdBy(tx.getCreatedBy())
                .createdByName(tx.getCreatedByName())
                .createdAt(createdAt)
                .build();
    }

    private Map<String, Object> toFirestoreMap(LedgerTransaction tx) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tx.getId());
        map.put("householdId", tx.getHouseholdId());
        map.put("budgetId", tx.getBudgetId());
        map.put("idempotencyKey", tx.getIdempotencyKey());
        map.put("type", tx.getType());
        map.put("status", tx.getStatus());
        map.put("amountCents", tx.getAmountCents());
        map.put("balanceAfterCents", tx.getBalanceAfterCents());
        map.put("category", tx.getCategory());
        map.put("note", tx.getNote());
        map.put("linkedBudgetId", tx.getLinkedBudgetId());
        map.put("transferGroupId", tx.getTransferGroupId());
        map.put("exchangeRate", tx.getExchangeRate());
        map.put("transactionDate", tx.getTransactionDate());
        map.put("createdBy", tx.getCreatedBy());
        map.put("createdByName", tx.getCreatedByName());
        map.put("createdAt", tx.getCreatedAt());
        return map;
    }

    private LedgerTransaction fromSnapshot(DocumentSnapshot doc) {
        LedgerTransaction tx = new LedgerTransaction();
        tx.setId(doc.getId());
        tx.setHouseholdId(doc.getString("householdId"));
        tx.setBudgetId(doc.getString("budgetId"));
        tx.setIdempotencyKey(doc.getString("idempotencyKey"));
        tx.setType(doc.getString("type"));
        tx.setStatus(doc.getString("status"));

        Long amount = doc.getLong("amountCents");
        tx.setAmountCents(amount != null ? amount : 0L);

        Long balanceAfter = doc.getLong("balanceAfterCents");
        tx.setBalanceAfterCents(balanceAfter != null ? balanceAfter : 0L);

        tx.setCategory(doc.getString("category"));
        tx.setNote(doc.getString("note"));
        tx.setLinkedBudgetId(doc.getString("linkedBudgetId"));
        tx.setTransferGroupId(doc.getString("transferGroupId"));
        tx.setExchangeRate(doc.getDouble("exchangeRate"));
        tx.setTransactionDate(doc.getTimestamp("transactionDate"));
        tx.setCreatedBy(doc.getString("createdBy"));
        tx.setCreatedByName(doc.getString("createdByName"));
        tx.setCreatedAt(doc.getTimestamp("createdAt"));
        return tx;
    }
}

