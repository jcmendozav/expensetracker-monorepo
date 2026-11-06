package com.house.expensetracker.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.house.expensetracker.dto.TransactionDto;
import com.house.expensetracker.event.BudgetLowBalanceEvent;
import com.house.expensetracker.event.TransactionCreatedEvent;
import com.house.expensetracker.model.Household;
import com.house.expensetracker.model.Notification;
import com.house.expensetracker.service.HouseholdService;
import com.house.expensetracker.service.NotificationService;

@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final HouseholdService householdService;

    @Autowired
    public NotificationEventListener(NotificationService notificationService, HouseholdService householdService) {
        this.notificationService = notificationService;
        this.householdService = householdService;
    }

    @Async
    @EventListener
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        TransactionDto tx = event.getTransaction();
        if (tx == null) {
            return;
        }

        try {
            Household household = householdService.getHouseholdEntity(tx.getHouseholdId());
            if (household == null || household.getMemberIds() == null) {
                return;
            }

            String actorId = tx.getCreatedBy();
            String actorName = tx.getCreatedByName() != null && !tx.getCreatedByName().isBlank()
                    ? tx.getCreatedByName()
                    : "Partner";

            double amountFormatted = tx.getAmountCents() / 100.0;
            String type = "SPENDING".equalsIgnoreCase(tx.getType()) ? "EXPENSE_LOGGED" : "FUNDS_ADDED";
            String title = "SPENDING".equalsIgnoreCase(tx.getType()) ? "Expense Logged" : "Funds Added";
            String message = String.format("%s logged %.2f on \"%s\"", actorName, amountFormatted, event.getBudgetTitle());

            // Fan out to other household members (actor excluded)
            for (String memberId : household.getMemberIds()) {
                if (memberId.equals(actorId)) {
                    continue;
                }

                Notification notification = Notification.builder()
                        .householdId(household.getId())
                        .recipientId(memberId)
                        .actorId(actorId)
                        .budgetId(tx.getBudgetId())
                        .budgetTitle(event.getBudgetTitle())
                        .type(type)
                        .title(title)
                        .message(message)
                        .isRead(false)
                        .build();

                notificationService.createNotification(notification);
            }

            // If budget is now depleted, send BUDGET_DEPLETED alert to all household members
            if ("DEPLETED".equalsIgnoreCase(event.getBudgetStatus()) || event.getCurrentBalanceCents() <= 0) {
                String depletedMsg = String.format("\"%s\" budget has reached 0.00", event.getBudgetTitle());
                for (String memberId : household.getMemberIds()) {
                    Notification depletedNotification = Notification.builder()
                            .householdId(household.getId())
                            .recipientId(memberId)
                            .actorId(actorId)
                            .budgetId(tx.getBudgetId())
                            .budgetTitle(event.getBudgetTitle())
                            .type("BUDGET_DEPLETED")
                            .title("Budget Depleted")
                            .message(depletedMsg)
                            .isRead(false)
                            .build();

                    notificationService.createNotification(depletedNotification);
                }
            }
        } catch (Exception e) {
            // Non-blocking catch
            System.err.println("Failed to dispatch transaction created notification: " + e.getMessage());
        }
    }

    @Async
    @EventListener
    public void handleBudgetLowBalance(BudgetLowBalanceEvent event) {
        try {
            Household household = householdService.getHouseholdEntity(event.getHouseholdId());
            if (household == null || household.getMemberIds() == null) {
                return;
            }

            String message = String.format("\"%s\" dropped below 10%% (remaining: %.2f)",
                    event.getBudgetTitle(), event.getCurrentBalanceCents() / 100.0);

            for (String memberId : household.getMemberIds()) {
                Notification notification = Notification.builder()
                        .householdId(household.getId())
                        .recipientId(memberId)
                        .actorId(event.getActorId())
                        .budgetId(event.getBudgetId())
                        .budgetTitle(event.getBudgetTitle())
                        .type("LOW_BALANCE_WARNING")
                        .title("Low Balance Alert")
                        .message(message)
                        .isRead(false)
                        .build();

                notificationService.createNotification(notification);
            }
        } catch (Exception e) {
            System.err.println("Failed to dispatch low balance alert: " + e.getMessage());
        }
    }
}
