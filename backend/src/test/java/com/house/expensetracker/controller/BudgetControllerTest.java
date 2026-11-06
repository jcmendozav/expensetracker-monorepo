package com.house.expensetracker.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
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

import com.house.expensetracker.dto.BudgetDto;
import com.house.expensetracker.dto.BudgetListResponse;
import com.house.expensetracker.dto.CreateBudgetRequest;
import com.house.expensetracker.dto.LiquiditySummaryResponse;
import com.house.expensetracker.dto.UpdateBudgetRequest;
import com.house.expensetracker.exception.ForbiddenException;
import com.house.expensetracker.exception.GlobalExceptionHandler;
import com.house.expensetracker.service.BudgetService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        BudgetController controller = new BudgetController(budgetService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/budgets with valid payload returns 201 Created and BudgetDto")
    void createBudget_validPayload_returns201() throws Exception {
        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .householdId("hh_123")
                .title("Kitchen Renovation")
                .initialAmountCents(500000L)
                .currency("PEN")
                .build();

        BudgetDto expected = BudgetDto.builder()
                .id("bdg_1")
                .householdId("hh_123")
                .title("Kitchen Renovation")
                .initialAmountCents(500000L)
                .currentBalanceCents(500000L)
                .currency("PEN")
                .status("ACTIVE")
                .ownerId("usr_carlos_123")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(budgetService.createBudget(any(CreateBudgetRequest.class), eq("usr_carlos_123")))
                .willReturn(expected);

        mockMvc.perform(post("/api/v1/budgets")
                .principal(() -> "usr_carlos_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("bdg_1"))
                .andExpect(jsonPath("$.title").value("Kitchen Renovation"))
                .andExpect(jsonPath("$.initialAmountCents").value(500000L))
                .andExpect(jsonPath("$.currentBalanceCents").value(500000L))
                .andExpect(jsonPath("$.currency").value("PEN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/budgets with invalid cents (<=0) returns 400 Bad Request")
    void createBudget_invalidCents_returns400() throws Exception {
        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .householdId("hh_123")
                .title("Invalid Budget")
                .initialAmountCents(0L)
                .currency("PEN")
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                .principal(() -> "usr_carlos_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.initialAmountCents").exists());
    }

    @Test
    @DisplayName("GET /api/v1/budgets returns 200 OK and list of budgets")
    void listBudgets_returns200() throws Exception {
        BudgetDto b1 = BudgetDto.builder()
                .id("bdg_1")
                .householdId("hh_123")
                .title("Groceries")
                .initialAmountCents(200000L)
                .currentBalanceCents(18000L)
                .currency("PEN")
                .status("LOW_BALANCE")
                .build();

        BudgetListResponse response = BudgetListResponse.builder()
                .items(List.of(b1))
                .nextCursor(null)
                .build();

        given(budgetService.listBudgets("hh_123", null, "usr_carlos_123")).willReturn(response);

        mockMvc.perform(get("/api/v1/budgets")
                .param("householdId", "hh_123")
                .principal(() -> "usr_carlos_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("bdg_1"))
                .andExpect(jsonPath("$.items[0].title").value("Groceries"))
                .andExpect(jsonPath("$.items[0].status").value("LOW_BALANCE"));
    }

    @Test
    @DisplayName("GET /api/v1/budgets/summary returns 200 OK with consolidated liquidity")
    void getSummary_returns200() throws Exception {
        LiquiditySummaryResponse response = LiquiditySummaryResponse.builder()
                .balancesByCurrency(Map.of("PEN", 1420000L, "USD", 85000L))
                .activeCount(4)
                .build();

        given(budgetService.getLiquiditySummary("hh_123", "usr_carlos_123")).willReturn(response);

        mockMvc.perform(get("/api/v1/budgets/summary")
                .param("householdId", "hh_123")
                .principal(() -> "usr_carlos_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balancesByCurrency.PEN").value(1420000L))
                .andExpect(jsonPath("$.balancesByCurrency.USD").value(85000L))
                .andExpect(jsonPath("$.activeCount").value(4));
    }

    @Test
    @DisplayName("PATCH /api/v1/budgets/{id} returns 200 OK on successful update")
    void updateBudget_valid_returns200() throws Exception {
        UpdateBudgetRequest request = UpdateBudgetRequest.builder()
                .title("Updated Title")
                .status("ARCHIVED")
                .build();

        BudgetDto updated = BudgetDto.builder()
                .id("bdg_1")
                .title("Updated Title")
                .status("ARCHIVED")
                .build();

        given(budgetService.updateBudget(eq("bdg_1"), any(UpdateBudgetRequest.class), eq("usr_carlos_123")))
                .willReturn(updated);

        mockMvc.perform(patch("/api/v1/budgets/bdg_1")
                .principal(() -> "usr_carlos_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/budgets/{id} returns 403 Forbidden when unauthorized")
    void updateBudget_forbidden_returns403() throws Exception {
        UpdateBudgetRequest request = UpdateBudgetRequest.builder()
                .title("Unauthorized Title")
                .build();

        given(budgetService.updateBudget(eq("bdg_1"), any(UpdateBudgetRequest.class), eq("usr_stranger_999")))
                .willThrow(new ForbiddenException("Not authorized"));

        mockMvc.perform(patch("/api/v1/budgets/bdg_1")
                .principal(() -> "usr_stranger_999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
