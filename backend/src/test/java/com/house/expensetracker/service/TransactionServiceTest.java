package com.house.expensetracker.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.google.api.core.ApiFutures;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import com.house.expensetracker.dto.LogTransactionRequest;
import com.house.expensetracker.dto.TransactionDto;
import com.house.expensetracker.dto.TransferRequest;
import com.house.expensetracker.dto.TransferResultDto;
import com.house.expensetracker.dto.UpdateTransactionMetadataRequest;
import com.house.expensetracker.event.BudgetLowBalanceEvent;
import com.house.expensetracker.event.TransactionCreatedEvent;
import com.house.expensetracker.exception.BadRequestException;
import com.house.expensetracker.model.Budget;
import com.house.expensetracker.model.Household;
import com.house.expensetracker.model.LedgerTransaction;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private Firestore firestore;

    @Mock
    private BudgetService budgetService;

    @Mock
    private HouseholdService householdService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CollectionReference budgetCollection;

    @Mock
    private CollectionReference householdCollection;

    @Mock
    private CollectionReference transactionCollection;

    @Mock
    private DocumentReference budgetDocRef;

    @Mock
    private DocumentReference targetBudgetDocRef;

    @Mock
    private DocumentReference householdDocRef;

    @Mock
    private DocumentReference transactionDocRef;

    @Mock
    private DocumentReference targetTransactionDocRef;

    @Mock
    private QuerySnapshot querySnapshot;

    @Mock
    private QueryDocumentSnapshot queryDocSnapshot;

    @Mock
    private Query query;

    @Mock
    private WriteBatch writeBatch;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = spy(new TransactionService(firestore, budgetService, householdService, eventPublisher));
    }

    private void setupBudgetSubcollection(String budgetId) {
        given(firestore.collection(BudgetService.COLLECTION_NAME)).willReturn(budgetCollection);
        given(budgetCollection.document(budgetId)).willReturn(budgetDocRef);
        given(budgetDocRef.collection(TransactionService.TRANSACTIONS_SUBCOLLECTION)).willReturn(transactionCollection);
    }

    @Test
    @DisplayName("commitTransaction commits SPENDING atomically, adjusts balances and publishes events")
    void commitTransaction_spending_commitsBatchAndPublishesEvents() {
        String budgetId = "bdg_groceries";
        String householdId = "hh_123";

        Budget budget = Budget.builder()
                .id(budgetId)
                .householdId(householdId)
                .title("Groceries")
                .currency("PEN")
                .initialAmountCents(100000L)
                .currentBalanceCents(100000L)
                .status("ACTIVE")
                .build();

        Household household = Household.builder()
                .id(householdId)
                .name("Mendoza Family")
                .memberIds(List.of("usr_carlos_123"))
                .balancesByCurrency(new HashMap<>(Map.of("PEN", 100000L)))
                .activeBudgetCount(1)
                .build();

        given(budgetService.getBudgetEntity(budgetId)).willReturn(budget);
        given(householdService.getHouseholdEntity(householdId)).willReturn(household);

        setupBudgetSubcollection(budgetId);
        given(transactionCollection.document()).willReturn(transactionDocRef);
        given(transactionDocRef.getId()).willReturn("tx_001");

        given(firestore.collection(HouseholdService.COLLECTION_NAME)).willReturn(householdCollection);
        given(householdCollection.document(householdId)).willReturn(householdDocRef);

        given(firestore.batch()).willReturn(writeBatch);
        given(writeBatch.commit()).willReturn(ApiFutures.immediateFuture(List.<WriteResult>of()));

        // Spending 95000 cents leaves 5000 cents (5%), which triggers LOW_BALANCE (< 10%)
        LogTransactionRequest request = LogTransactionRequest.builder()
                .type("SPENDING")
                .amountCents(95000L)
                .category("Supermarket")
                .note("Weekly groceries")
                .build();

        TransactionDto dto = transactionService.commitTransaction(budgetId, request, "usr_carlos_123", "Carlos Mendoza");

        assertThat(dto.getId()).isEqualTo("tx_001");
        assertThat(dto.getType()).isEqualTo("SPENDING");
        assertThat(dto.getAmountCents()).isEqualTo(95000L);
        assertThat(dto.getBalanceAfterCents()).isEqualTo(5000L);
        assertThat(dto.getCreatedByName()).isEqualTo("Carlos Mendoza");

        verify(writeBatch).set(eq(transactionDocRef), anyMap());
        verify(writeBatch).update(eq(budgetDocRef), eq("currentBalanceCents"), eq(5000L), eq("status"), eq("LOW_BALANCE"), eq("updatedAt"), any());
        verify(writeBatch).update(eq(householdDocRef), eq("balancesByCurrency"), eq(Map.of("PEN", 5000L)), eq("activeBudgetCount"), eq(1), eq("updatedAt"), any());

        verify(eventPublisher).publishEvent(any(TransactionCreatedEvent.class));
        verify(eventPublisher).publishEvent(any(BudgetLowBalanceEvent.class));
    }

    @Test
    @DisplayName("commitTransaction commits INCOME atomically and increments balance")
    void commitTransaction_income_incrementsBalance() {
        String budgetId = "bdg_savings";
        String householdId = "hh_123";

        Budget budget = Budget.builder()
                .id(budgetId)
                .householdId(householdId)
                .title("Savings")
                .currency("USD")
                .initialAmountCents(50000L)
                .currentBalanceCents(50000L)
                .status("ACTIVE")
                .build();

        Household household = Household.builder()
                .id(householdId)
                .name("Mendoza Family")
                .memberIds(List.of("usr_carlos_123"))
                .balancesByCurrency(new HashMap<>(Map.of("USD", 50000L)))
                .activeBudgetCount(1)
                .build();

        given(budgetService.getBudgetEntity(budgetId)).willReturn(budget);
        given(householdService.getHouseholdEntity(householdId)).willReturn(household);

        setupBudgetSubcollection(budgetId);
        given(transactionCollection.document()).willReturn(transactionDocRef);
        given(transactionDocRef.getId()).willReturn("tx_002");

        given(firestore.collection(HouseholdService.COLLECTION_NAME)).willReturn(householdCollection);
        given(householdCollection.document(householdId)).willReturn(householdDocRef);

        given(firestore.batch()).willReturn(writeBatch);
        given(writeBatch.commit()).willReturn(ApiFutures.immediateFuture(List.<WriteResult>of()));

        LogTransactionRequest request = LogTransactionRequest.builder()
                .type("INCOME")
                .amountCents(25000L)
                .category("Top-up")
                .note("Paycheck allocation")
                .build();

        TransactionDto dto = transactionService.commitTransaction(budgetId, request, "usr_carlos_123", "Carlos");

        assertThat(dto.getAmountCents()).isEqualTo(25000L);
        assertThat(dto.getBalanceAfterCents()).isEqualTo(75000L);

        verify(writeBatch).update(eq(budgetDocRef), eq("currentBalanceCents"), eq(75000L), eq("status"), eq("ACTIVE"), eq("updatedAt"), any());
        verify(writeBatch).update(eq(householdDocRef), eq("balancesByCurrency"), eq(Map.of("USD", 75000L)), eq("activeBudgetCount"), eq(1), eq("updatedAt"), any());
        verify(eventPublisher).publishEvent(any(TransactionCreatedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(BudgetLowBalanceEvent.class));
    }

    @Test
    @DisplayName("commitTransaction throws BadRequestException when transaction type is invalid")
    void commitTransaction_invalidType_throwsBadRequestException() {
        Budget budget = Budget.builder().id("b1").householdId("hh1").build();
        Household household = Household.builder().id("hh1").build();
        given(budgetService.getBudgetEntity("b1")).willReturn(budget);
        given(householdService.getHouseholdEntity("hh1")).willReturn(household);

        LogTransactionRequest request = LogTransactionRequest.builder()
                .type("INVALID_TYPE")
                .amountCents(1000L)
                .category("Misc")
                .build();

        assertThatThrownBy(() -> transactionService.commitTransaction("b1", request, "usr_1", "User"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid transaction type");
    }

    @Test
    @DisplayName("commitTransaction throws BadRequestException when caller UID is missing")
    void commitTransaction_missingCallerUid_throwsBadRequestException() {
        LogTransactionRequest request = LogTransactionRequest.builder()
                .type("SPENDING")
                .amountCents(1000L)
                .category("Misc")
                .build();

        assertThatThrownBy(() -> transactionService.commitTransaction("b1", request, "", "User"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Caller UID is required");
    }

    @Test
    @DisplayName("listTransactions queries subcollection descending with specified limit")
    void listTransactions_queriesSubcollectionDescending() {
        String budgetId = "bdg_1";
        Budget budget = Budget.builder().id(budgetId).householdId("hh_1").build();
        Household household = Household.builder().id("hh_1").build();
        given(budgetService.getBudgetEntity(budgetId)).willReturn(budget);
        given(householdService.getHouseholdEntity("hh_1")).willReturn(household);

        setupBudgetSubcollection(budgetId);
        given(transactionCollection.orderBy("transactionDate", Query.Direction.DESCENDING)).willReturn(query);
        given(query.limit(20)).willReturn(query);
        given(query.get()).willReturn(ApiFutures.immediateFuture(querySnapshot));
        given(querySnapshot.getDocuments()).willReturn(List.of(queryDocSnapshot));

        LedgerTransaction tx = LedgerTransaction.builder()
                .id("tx_01")
                .householdId("hh_1")
                .budgetId(budgetId)
                .type("SPENDING")
                .status("COMMITTED")
                .amountCents(5000L)
                .balanceAfterCents(45000L)
                .category("Food")
                .build();

        doReturn(tx).when(transactionService).fromSnapshot(queryDocSnapshot);

        List<TransactionDto> result = transactionService.listTransactions(budgetId, 20, "usr_1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("tx_01");
        assertThat(result.get(0).getAmountCents()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("updateMetadata updates category and note without altering balance")
    void updateMetadata_updatesCategoryAndNote() {
        String budgetId = "bdg_1";
        String txId = "tx_01";
        Budget budget = Budget.builder().id(budgetId).householdId("hh_1").build();
        Household household = Household.builder().id("hh_1").build();
        given(budgetService.getBudgetEntity(budgetId)).willReturn(budget);
        given(householdService.getHouseholdEntity("hh_1")).willReturn(household);

        setupBudgetSubcollection(budgetId);
        given(transactionCollection.document(txId)).willReturn(transactionDocRef);

        LedgerTransaction existing = LedgerTransaction.builder()
                .id(txId)
                .householdId("hh_1")
                .budgetId(budgetId)
                .type("SPENDING")
                .status("COMMITTED")
                .amountCents(3000L)
                .category("Old Category")
                .note("Old Note")
                .build();

        doReturn(existing).when(transactionService).getTransactionEntity(budgetId, txId);
        given(transactionDocRef.update("category", "New Category", "note", "New Note"))
                .willReturn(ApiFutures.immediateFuture(null));

        UpdateTransactionMetadataRequest request = UpdateTransactionMetadataRequest.builder()
                .category("New Category")
                .note("New Note")
                .build();

        TransactionDto updated = transactionService.updateMetadata(budgetId, txId, request, "usr_1");

        assertThat(updated.getCategory()).isEqualTo("New Category");
        assertThat(updated.getNote()).isEqualTo("New Note");
        verify(transactionDocRef).update("category", "New Category", "note", "New Note");
    }

    @Test
    @DisplayName("voidTransaction is idempotent when transaction is already VOIDED")
    void voidTransaction_alreadyVoided_returnsWithoutDuplicateRefund() {
        String budgetId = "bdg_1";
        String txId = "tx_01";
        Budget budget = Budget.builder().id(budgetId).householdId("hh_1").build();
        Household household = Household.builder().id("hh_1").build();
        given(budgetService.getBudgetEntity(budgetId)).willReturn(budget);
        given(householdService.getHouseholdEntity("hh_1")).willReturn(household);

        setupBudgetSubcollection(budgetId);
        given(transactionCollection.document(txId)).willReturn(transactionDocRef);

        LedgerTransaction tx = LedgerTransaction.builder()
                .id(txId)
                .householdId("hh_1")
                .budgetId(budgetId)
                .type("SPENDING")
                .status("VOIDED")
                .amountCents(5000L)
                .build();

        doReturn(tx).when(transactionService).getTransactionEntity(budgetId, txId);

        TransactionDto result = transactionService.voidTransaction(budgetId, txId, "usr_1");

        assertThat(result.getStatus()).isEqualTo("VOIDED");
        verify(firestore, never()).batch();
    }

    @Test
    @DisplayName("voidTransaction refunds SPENDING amount atomically back to budget and household balances")
    void voidTransaction_spending_refundsAmountAtomically() {
        String budgetId = "bdg_1";
        String txId = "tx_01";
        String householdId = "hh_1";

        Budget budget = Budget.builder()
                .id(budgetId)
                .householdId(householdId)
                .currency("PEN")
                .initialAmountCents(100000L)
                .currentBalanceCents(20000L)
                .status("LOW_BALANCE")
                .build();

        Household household = Household.builder()
                .id(householdId)
                .balancesByCurrency(new HashMap<>(Map.of("PEN", 20000L)))
                .activeBudgetCount(1)
                .build();

        given(budgetService.getBudgetEntity(budgetId)).willReturn(budget);
        given(householdService.getHouseholdEntity(householdId)).willReturn(household);

        setupBudgetSubcollection(budgetId);
        given(transactionCollection.document(txId)).willReturn(transactionDocRef);

        LedgerTransaction tx = LedgerTransaction.builder()
                .id(txId)
                .householdId(householdId)
                .budgetId(budgetId)
                .type("SPENDING")
                .status("COMMITTED")
                .amountCents(30000L)
                .build();

        doReturn(tx).when(transactionService).getTransactionEntity(budgetId, txId);

        given(firestore.collection(HouseholdService.COLLECTION_NAME)).willReturn(householdCollection);
        given(householdCollection.document(householdId)).willReturn(householdDocRef);

        given(firestore.batch()).willReturn(writeBatch);
        given(writeBatch.commit()).willReturn(ApiFutures.immediateFuture(List.<WriteResult>of()));

        TransactionDto result = transactionService.voidTransaction(budgetId, txId, "usr_1");

        assertThat(result.getStatus()).isEqualTo("VOIDED");
        // Refunding 30000 to 20000 brings balance to 50000 (50%), returning status to ACTIVE
        verify(writeBatch).update(eq(transactionDocRef), eq("status"), eq("VOIDED"));
        verify(writeBatch).update(eq(budgetDocRef), eq("currentBalanceCents"), eq(50000L), eq("status"), eq("ACTIVE"), eq("updatedAt"), any());
        verify(writeBatch).update(eq(householdDocRef), eq("balancesByCurrency"), eq(Map.of("PEN", 50000L)), eq("activeBudgetCount"), eq(1), eq("updatedAt"), any());
    }

    @Test
    @DisplayName("transferFunds successfully moves funds cross-currency with rounded FX conversion")
    void transferFunds_crossCurrency_computesRoundedTargetAmountAndCommitsAtomically() {
        String sourceBudgetId = "bdg_pen";
        String targetBudgetId = "bdg_usd";
        String householdId = "hh_123";

        Budget sourceBudget = Budget.builder()
                .id(sourceBudgetId)
                .householdId(householdId)
                .title("PEN Budget")
                .currency("PEN")
                .initialAmountCents(200000L)
                .currentBalanceCents(100000L)
                .status("ACTIVE")
                .build();

        Budget targetBudget = Budget.builder()
                .id(targetBudgetId)
                .householdId(householdId)
                .title("USD Budget")
                .currency("USD")
                .initialAmountCents(100000L)
                .currentBalanceCents(50000L)
                .status("ACTIVE")
                .build();

        Household household = Household.builder()
                .id(householdId)
                .memberIds(List.of("usr_carlos_123"))
                .balancesByCurrency(new HashMap<>(Map.of("PEN", 100000L, "USD", 50000L)))
                .activeBudgetCount(2)
                .build();

        given(budgetService.getBudgetEntity(sourceBudgetId)).willReturn(sourceBudget);
        given(budgetService.getBudgetEntity(targetBudgetId)).willReturn(targetBudget);
        given(householdService.getHouseholdEntity(householdId)).willReturn(household);

        given(firestore.collection(BudgetService.COLLECTION_NAME)).willReturn(budgetCollection);
        given(budgetCollection.document(sourceBudgetId)).willReturn(budgetDocRef);
        given(budgetCollection.document(targetBudgetId)).willReturn(targetBudgetDocRef);

        given(budgetDocRef.collection(TransactionService.TRANSACTIONS_SUBCOLLECTION)).willReturn(transactionCollection);
        CollectionReference targetTxCollection = org.mockito.Mockito.mock(CollectionReference.class);
        given(targetBudgetDocRef.collection(TransactionService.TRANSACTIONS_SUBCOLLECTION)).willReturn(targetTxCollection);

        given(transactionCollection.document()).willReturn(transactionDocRef);
        given(targetTxCollection.document()).willReturn(targetTransactionDocRef);
        given(transactionDocRef.getId()).willReturn("tx_source_out");
        given(targetTransactionDocRef.getId()).willReturn("tx_target_in");

        given(firestore.collection(HouseholdService.COLLECTION_NAME)).willReturn(householdCollection);
        given(householdCollection.document(householdId)).willReturn(householdDocRef);

        given(firestore.batch()).willReturn(writeBatch);
        given(writeBatch.commit()).willReturn(ApiFutures.immediateFuture(List.<WriteResult>of()));

        // Transfer 37000 PEN to USD with rate 0.27 -> Math.round(37000 * 0.27) = 9990 USD cents
        TransferRequest request = TransferRequest.builder()
                .sourceBudgetId(sourceBudgetId)
                .targetBudgetId(targetBudgetId)
                .sourceAmountCents(37000L)
                .exchangeRate(0.27)
                .note("Transfer to USD emergency fund")
                .build();

        TransferResultDto result = transactionService.transferFunds(request, "usr_carlos_123", "Carlos");

        assertThat(result.getTransferGroupId()).isNotBlank();
        assertThat(result.getSourceRemainingBalanceCents()).isEqualTo(63000L);
        assertThat(result.getTargetRemainingBalanceCents()).isEqualTo(59990L);
        assertThat(result.getSourceTransaction().getAmountCents()).isEqualTo(37000L);
        assertThat(result.getSourceTransaction().getType()).isEqualTo("TRANSFER_OUT");
        assertThat(result.getTargetTransaction().getAmountCents()).isEqualTo(9990L);
        assertThat(result.getTargetTransaction().getType()).isEqualTo("TRANSFER_IN");

        verify(writeBatch).set(eq(transactionDocRef), anyMap());
        verify(writeBatch).set(eq(targetTransactionDocRef), anyMap());
        verify(writeBatch).update(eq(budgetDocRef), eq("currentBalanceCents"), eq(63000L), eq("status"), eq("ACTIVE"), eq("updatedAt"), any());
        verify(writeBatch).update(eq(targetBudgetDocRef), eq("currentBalanceCents"), eq(59990L), eq("status"), eq("ACTIVE"), eq("updatedAt"), any());
        verify(writeBatch).update(eq(householdDocRef), eq("balancesByCurrency"), eq(Map.of("PEN", 63000L, "USD", 59990L)), eq("activeBudgetCount"), eq(2), eq("updatedAt"), any());
    }

    @Test
    @DisplayName("transferFunds throws BadRequestException when source budget has insufficient funds")
    void transferFunds_insufficientFunds_throwsBadRequestException() {
        Budget sourceBudget = Budget.builder()
                .id("b1")
                .householdId("hh1")
                .title("Source")
                .currency("PEN")
                .currentBalanceCents(5000L)
                .build();

        Budget targetBudget = Budget.builder()
                .id("b2")
                .householdId("hh1")
                .title("Target")
                .currency("PEN")
                .currentBalanceCents(10000L)
                .build();

        given(budgetService.getBudgetEntity("b1")).willReturn(sourceBudget);
        given(budgetService.getBudgetEntity("b2")).willReturn(targetBudget);
        given(householdService.getHouseholdEntity("hh1")).willReturn(Household.builder().id("hh1").build());

        TransferRequest request = TransferRequest.builder()
                .sourceBudgetId("b1")
                .targetBudgetId("b2")
                .sourceAmountCents(10000L)
                .build();

        assertThatThrownBy(() -> transactionService.transferFunds(request, "usr_1", "User"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("transferFunds throws BadRequestException when source and target are the same")
    void transferFunds_sameBudget_throwsBadRequestException() {
        Budget b1 = Budget.builder().id("b1").householdId("hh1").build();
        given(budgetService.getBudgetEntity("b1")).willReturn(b1);

        TransferRequest request = TransferRequest.builder()
                .sourceBudgetId("b1")
                .targetBudgetId("b1")
                .sourceAmountCents(1000L)
                .build();

        assertThatThrownBy(() -> transactionService.transferFunds(request, "usr_1", "User"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Source and target budgets must be different");
    }

    @Test
    @DisplayName("transferFunds throws BadRequestException when exchange rate is missing for cross currency transfer")
    void transferFunds_missingExchangeRate_throwsBadRequestException() {
        Budget sourceBudget = Budget.builder().id("b1").householdId("hh1").currency("PEN").currentBalanceCents(10000L).build();
        Budget targetBudget = Budget.builder().id("b2").householdId("hh1").currency("USD").currentBalanceCents(10000L).build();

        given(budgetService.getBudgetEntity("b1")).willReturn(sourceBudget);
        given(budgetService.getBudgetEntity("b2")).willReturn(targetBudget);
        given(householdService.getHouseholdEntity("hh1")).willReturn(Household.builder().id("hh1").build());

        TransferRequest request = TransferRequest.builder()
                .sourceBudgetId("b1")
                .targetBudgetId("b2")
                .sourceAmountCents(5000L)
                .exchangeRate(null)
                .build();

        assertThatThrownBy(() -> transactionService.transferFunds(request, "usr_1", "User"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Exchange rate is required");
    }
}
