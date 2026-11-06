package com.house.expensetracker.controller;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.house.expensetracker.dto.LogTransactionRequest;
import com.house.expensetracker.dto.TransactionDto;
import com.house.expensetracker.dto.TransferRequest;
import com.house.expensetracker.dto.TransferResultDto;
import com.house.expensetracker.dto.UpdateTransactionMetadataRequest;
import com.house.expensetracker.exception.GlobalExceptionHandler;
import com.house.expensetracker.security.AuthenticatedUser;
import com.house.expensetracker.service.TransactionService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        TransactionController controller = new TransactionController(transactionService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/budgets/{id}/transactions with valid payload returns 201 Created")
    void logTransaction_validPayload_returns201() throws Exception {
        LogTransactionRequest request = LogTransactionRequest.builder()
                .type("SPENDING")
                .amountCents(4500L)
                .category("Supermarket")
                .note("Snacks")
                .build();

        TransactionDto expectedDto = TransactionDto.builder()
                .id("tx_1")
                .householdId("hh_123")
                .budgetId("bdg_1")
                .type("SPENDING")
                .status("COMMITTED")
                .amountCents(4500L)
                .balanceAfterCents(95500L)
                .category("Supermarket")
                .note("Snacks")
                .createdBy("usr_carlos_123")
                .createdByName("Carlos Mendoza")
                .createdAt(Instant.now())
                .build();

        given(transactionService.commitTransaction(
                eq("bdg_1"),
                any(LogTransactionRequest.class),
                eq("usr_carlos_123"),
                eq("Carlos Mendoza")))
                .willReturn(expectedDto);

        mockMvc.perform(post("/api/v1/budgets/bdg_1/transactions")
                .principal(new AuthenticatedUser("usr_carlos_123", "carlos@example.com", "Carlos Mendoza"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("tx_1"))
                .andExpect(jsonPath("$.amountCents").value(4500L))
                .andExpect(jsonPath("$.type").value("SPENDING"))
                .andExpect(jsonPath("$.category").value("Supermarket"))
                .andExpect(jsonPath("$.createdByName").value("Carlos Mendoza"));
    }

    @Test
    @DisplayName("POST /api/v1/budgets/{id}/transactions with invalid amount (<=0) returns 400 Bad Request")
    void logTransaction_invalidAmount_returns400() throws Exception {
        LogTransactionRequest request = LogTransactionRequest.builder()
                .type("SPENDING")
                .amountCents(0L)
                .category("Supermarket")
                .build();

        mockMvc.perform(post("/api/v1/budgets/bdg_1/transactions")
                .principal(new AuthenticatedUser("usr_carlos_123", "carlos@example.com", "Carlos Mendoza"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/budgets/{id}/transactions returns 200 OK and list of transactions")
    void listTransactions_returns200() throws Exception {
        TransactionDto tx = TransactionDto.builder()
                .id("tx_1")
                .householdId("hh_123")
                .budgetId("bdg_1")
                .type("SPENDING")
                .status("COMMITTED")
                .amountCents(2000L)
                .build();

        given(transactionService.listTransactions(eq("bdg_1"), eq(50), eq("usr_carlos_123")))
                .willReturn(List.of(tx));

        mockMvc.perform(get("/api/v1/budgets/bdg_1/transactions")
                .principal(new AuthenticatedUser("usr_carlos_123", "carlos@example.com", "Carlos Mendoza")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("tx_1"))
                .andExpect(jsonPath("$[0].amountCents").value(2000L));
    }

    @Test
    @DisplayName("PATCH /api/v1/budgets/{id}/transactions/{txId} updates metadata and returns 200 OK")
    void updateTransactionMetadata_returns200() throws Exception {
        UpdateTransactionMetadataRequest request = UpdateTransactionMetadataRequest.builder()
                .category("Utilities")
                .note("Electricity bill")
                .build();

        TransactionDto updated = TransactionDto.builder()
                .id("tx_1")
                .category("Utilities")
                .note("Electricity bill")
                .build();

        given(transactionService.updateMetadata(eq("bdg_1"), eq("tx_1"), any(UpdateTransactionMetadataRequest.class), eq("usr_carlos_123")))
                .willReturn(updated);

        mockMvc.perform(patch("/api/v1/budgets/bdg_1/transactions/tx_1")
                .principal(new AuthenticatedUser("usr_carlos_123", "carlos@example.com", "Carlos Mendoza"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Utilities"))
                .andExpect(jsonPath("$.note").value("Electricity bill"));
    }

    @Test
    @DisplayName("POST /api/v1/budgets/{id}/transactions/{txId}/void voids transaction and returns 200 OK")
    void voidTransaction_returns200() throws Exception {
        TransactionDto voided = TransactionDto.builder()
                .id("tx_1")
                .status("VOIDED")
                .amountCents(4500L)
                .build();

        given(transactionService.voidTransaction(eq("bdg_1"), eq("tx_1"), eq("usr_carlos_123")))
                .willReturn(voided);

        mockMvc.perform(post("/api/v1/budgets/bdg_1/transactions/tx_1/void")
                .principal(new AuthenticatedUser("usr_carlos_123", "carlos@example.com", "Carlos Mendoza")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("tx_1"))
                .andExpect(jsonPath("$.status").value("VOIDED"));
    }

    @Test
    @DisplayName("POST /api/v1/budgets/transfers transfers funds between budgets and returns 200 OK")
    void transferFunds_validPayload_returns200() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .sourceBudgetId("bdg_1")
                .targetBudgetId("bdg_2")
                .sourceAmountCents(10000L)
                .exchangeRate(1.0)
                .note("Rebalancing")
                .build();

        TransferResultDto result = TransferResultDto.builder()
                .transferGroupId("grp_123")
                .sourceRemainingBalanceCents(90000L)
                .targetRemainingBalanceCents(60000L)
                .build();

        given(transactionService.transferFunds(
                any(TransferRequest.class),
                eq("usr_carlos_123"),
                eq("Carlos Mendoza")))
                .willReturn(result);

        mockMvc.perform(post("/api/v1/budgets/transfers")
                .principal(new AuthenticatedUser("usr_carlos_123", "carlos@example.com", "Carlos Mendoza"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferGroupId").value("grp_123"))
                .andExpect(jsonPath("$.sourceRemainingBalanceCents").value(90000L))
                .andExpect(jsonPath("$.targetRemainingBalanceCents").value(60000L));
    }
}
