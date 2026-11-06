package com.house.expensetracker.service;

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

import com.google.api.core.ApiFutures;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import com.house.expensetracker.dto.BudgetDto;
import com.house.expensetracker.dto.CreateBudgetRequest;
import com.house.expensetracker.dto.LiquiditySummaryResponse;
import com.house.expensetracker.dto.UpdateBudgetRequest;
import com.house.expensetracker.exception.BadRequestException;
import com.house.expensetracker.model.Budget;
import com.house.expensetracker.model.Household;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private Firestore firestore;

    @Mock
    private HouseholdService householdService;

    @Mock
    private CollectionReference budgetCollection;

    @Mock
    private CollectionReference householdCollection;

    @Mock
    private DocumentReference budgetDocRef;

    @Mock
    private DocumentReference householdDocRef;

    @Mock
    private WriteBatch writeBatch;

    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetService = spy(new BudgetService(firestore, householdService));
    }

    @Test
    @DisplayName("createBudget atomically commits budget and updates household liquidity snapshot and activeBudgetCount")
    void createBudget_commitsAtomicBatchAndUpdatesHouseholdSnapshot() {
        Household household = Household.builder()
                .id("hh_123")
                .name("Mendoza Family")
                .memberIds(List.of("usr_carlos_123"))
                .balancesByCurrency(new HashMap<>(Map.of("PEN", 100000L)))
                .activeBudgetCount(1)
                .build();

        given(householdService.getHouseholdEntity("hh_123")).willReturn(household);
        given(firestore.collection(BudgetService.COLLECTION_NAME)).willReturn(budgetCollection);
        given(budgetCollection.document()).willReturn(budgetDocRef);
        given(budgetDocRef.getId()).willReturn("bdg_new");
        given(firestore.collection(HouseholdService.COLLECTION_NAME)).willReturn(householdCollection);
        given(householdCollection.document("hh_123")).willReturn(householdDocRef);
        given(firestore.batch()).willReturn(writeBatch);
        given(writeBatch.commit()).willReturn(ApiFutures.immediateFuture(List.<WriteResult>of()));

        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .householdId("hh_123")
                .title("Kitchen Renovation")
                .initialAmountCents(500000L)
                .currency("pen")
                .build();

        BudgetDto result = budgetService.createBudget(request, "usr_carlos_123");

        assertThat(result.getId()).isEqualTo("bdg_new");
        assertThat(result.getCurrency()).isEqualTo("PEN");
        assertThat(result.getInitialAmountCents()).isEqualTo(500000L);
        assertThat(result.getCurrentBalanceCents()).isEqualTo(500000L);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        assertThat(household.getBalancesByCurrency()).containsEntry("PEN", 600000L);
        assertThat(household.getActiveBudgetCount()).isEqualTo(2);

        verify(writeBatch).set(eq(budgetDocRef), anyMap());
        verify(writeBatch).update(
                eq(householdDocRef),
                eq("balancesByCurrency"), eq(Map.of("PEN", 600000L)),
                eq("activeBudgetCount"), eq(2),
                eq("updatedAt"), any(Timestamp.class));
        verify(writeBatch).commit();
    }

    @Test
    @DisplayName("updateBudget adjusts initialAmountCents, currentBalanceCents, and household snapshot atomically")
    void updateBudget_adjustsAllocationAndHouseholdSnapshot() {
        Budget existingBudget = Budget.builder()
                .id("bdg_1")
                .householdId("hh_123")
                .title("Kitchen Renovation")
                .initialAmountCents(500000L)
                .currentBalanceCents(345000L)
                .currency("PEN")
                .status("ACTIVE")
                .ownerId("usr_carlos_123")
                .createdAt(Timestamp.now())
                .updatedAt(Timestamp.now())
                .build();

        Household household = Household.builder()
                .id("hh_123")
                .memberIds(List.of("usr_carlos_123"))
                .balancesByCurrency(new HashMap<>(Map.of("PEN", 345000L)))
                .activeBudgetCount(1)
                .build();

        doReturn(existingBudget).when(budgetService).getBudgetEntity("bdg_1");
        given(householdService.getHouseholdEntity("hh_123")).willReturn(household);
        given(firestore.collection(BudgetService.COLLECTION_NAME)).willReturn(budgetCollection);
        given(budgetCollection.document("bdg_1")).willReturn(budgetDocRef);
        given(firestore.collection(HouseholdService.COLLECTION_NAME)).willReturn(householdCollection);
        given(householdCollection.document("hh_123")).willReturn(householdDocRef);
        given(firestore.batch()).willReturn(writeBatch);
        given(writeBatch.commit()).willReturn(ApiFutures.immediateFuture(List.<WriteResult>of()));

        UpdateBudgetRequest request = UpdateBudgetRequest.builder()
                .title("Kitchen & Appliances")
                .initialAmountCents(600000L) // +1000.00 PEN (+100000 cents)
                .build();

        BudgetDto updated = budgetService.updateBudget("bdg_1", request, "usr_carlos_123");

        assertThat(updated.getTitle()).isEqualTo("Kitchen & Appliances");
        assertThat(updated.getInitialAmountCents()).isEqualTo(600000L);
        assertThat(updated.getCurrentBalanceCents()).isEqualTo(445000L);
        assertThat(household.getBalancesByCurrency()).containsEntry("PEN", 445000L);
        verify(writeBatch).commit();
    }

    @Test
    @DisplayName("updateBudget to ARCHIVED removes remaining balance from household liquidity and decrements activeBudgetCount")
    void updateBudget_archive_removesLiquidityAndDecrementsActiveCount() {
        Budget existingBudget = Budget.builder()
                .id("bdg_1")
                .householdId("hh_123")
                .title("Kitchen Renovation")
                .initialAmountCents(500000L)
                .currentBalanceCents(120000L)
                .currency("PEN")
                .status("ACTIVE")
                .ownerId("usr_carlos_123")
                .createdAt(Timestamp.now())
                .updatedAt(Timestamp.now())
                .build();

        Household household = Household.builder()
                .id("hh_123")
                .memberIds(List.of("usr_carlos_123"))
                .balancesByCurrency(new HashMap<>(Map.of("PEN", 320000L)))
                .activeBudgetCount(2)
                .build();

        doReturn(existingBudget).when(budgetService).getBudgetEntity("bdg_1");
        given(householdService.getHouseholdEntity("hh_123")).willReturn(household);
        given(firestore.collection(BudgetService.COLLECTION_NAME)).willReturn(budgetCollection);
        given(budgetCollection.document("bdg_1")).willReturn(budgetDocRef);
        given(firestore.collection(HouseholdService.COLLECTION_NAME)).willReturn(householdCollection);
        given(householdCollection.document("hh_123")).willReturn(householdDocRef);
        given(firestore.batch()).willReturn(writeBatch);
        given(writeBatch.commit()).willReturn(ApiFutures.immediateFuture(List.<WriteResult>of()));

        UpdateBudgetRequest request = UpdateBudgetRequest.builder()
                .status("ARCHIVED")
                .build();

        BudgetDto updated = budgetService.updateBudget("bdg_1", request, "usr_carlos_123");

        assertThat(updated.getStatus()).isEqualTo("ARCHIVED");
        assertThat(household.getBalancesByCurrency()).containsEntry("PEN", 200000L);
        assertThat(household.getActiveBudgetCount()).isEqualTo(1);
        verify(writeBatch).commit();
    }

    @Test
    @DisplayName("getLiquiditySummary reads strictly 1 cached Household document without querying budgets collection")
    void getLiquiditySummary_readsSingleHouseholdDocument() {
        Household household = Household.builder()
                .id("hh_123")
                .memberIds(List.of("usr_carlos_123"))
                .balancesByCurrency(Map.of("PEN", 1420000L, "USD", 85000L))
                .activeBudgetCount(4)
                .build();

        given(householdService.getHouseholdEntity("hh_123")).willReturn(household);

        LiquiditySummaryResponse summary = budgetService.getLiquiditySummary("hh_123", "usr_carlos_123");

        assertThat(summary.getBalancesByCurrency())
                .containsEntry("PEN", 1420000L)
                .containsEntry("USD", 85000L);
        assertThat(summary.getActiveCount()).isEqualTo(4);

        // Verify zero reads on the budgets collection (§3.6 single-document invariant)
        verify(firestore, never()).collection(BudgetService.COLLECTION_NAME);
    }

    @Test
    @DisplayName("getLiquiditySummary throws BadRequestException when householdId is blank")
    void getLiquiditySummary_blankHouseholdId_throwsBadRequest() {
        assertThatThrownBy(() -> budgetService.getLiquiditySummary("  ", "usr_carlos_123"))
                .isInstanceOf(BadRequestException.class);
    }
}
