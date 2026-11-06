package com.house.expensetracker.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.house.expensetracker.dto.BudgetDto;
import com.house.expensetracker.dto.BudgetListResponse;
import com.house.expensetracker.dto.CreateBudgetRequest;
import com.house.expensetracker.dto.LiquiditySummaryResponse;
import com.house.expensetracker.dto.UpdateBudgetRequest;
import com.house.expensetracker.security.AuthenticatedUser;
import com.house.expensetracker.service.BudgetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    @Autowired
    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<BudgetDto> createBudget(
            @Valid @RequestBody CreateBudgetRequest request,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        BudgetDto created = budgetService.createBudget(request, caller.uid());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<BudgetListResponse> listBudgets(
            @RequestParam("householdId") String householdId,
            @RequestParam(value = "status", required = false) String status,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        BudgetListResponse response = budgetService.listBudgets(householdId, status, caller.uid());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<LiquiditySummaryResponse> getSummary(
            @RequestParam("householdId") String householdId,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        LiquiditySummaryResponse response = budgetService.getLiquiditySummary(householdId, caller.uid());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetDto> getBudgetById(
            @PathVariable("id") String id,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        BudgetDto budget = budgetService.getBudgetById(id, caller.uid());
        return ResponseEntity.ok(budget);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BudgetDto> updateBudget(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateBudgetRequest request,
            Principal principal) {
        AuthenticatedUser caller = AuthenticatedUser.from(principal);
        BudgetDto updated = budgetService.updateBudget(id, request, caller.uid());
        return ResponseEntity.ok(updated);
    }
}
