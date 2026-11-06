package com.house.expensetracker.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BudgetTest {

    @Test
    @DisplayName("deriveStatus: returns ACTIVE when balance is >= 10% of initial")
    void deriveStatus_healthyBalance_returnsActive() {
        // 5000 initial, 3450 balance (69%)
        String status = Budget.deriveStatus(345000L, 500000L, "ACTIVE");
        assertThat(status).isEqualTo("ACTIVE");

        // Exactly 10% (50000 cents of 500000)
        String statusAt10Percent = Budget.deriveStatus(50000L, 500000L, "ACTIVE");
        assertThat(statusAt10Percent).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("deriveStatus: returns LOW_BALANCE when balance drops below 10%")
    void deriveStatus_lowBalance_returnsLowBalance() {
        // 5000 initial, 499 balance (9.98%)
        String status = Budget.deriveStatus(49900L, 500000L, "ACTIVE");
        assertThat(status).isEqualTo("LOW_BALANCE");

        // 1 cent remaining
        String status1Cent = Budget.deriveStatus(1L, 500000L, "ACTIVE");
        assertThat(status1Cent).isEqualTo("LOW_BALANCE");
    }

    @Test
    @DisplayName("deriveStatus: returns DEPLETED when balance reaches zero or negative")
    void deriveStatus_zeroBalance_returnsDepleted() {
        String statusZero = Budget.deriveStatus(0L, 500000L, "ACTIVE");
        assertThat(statusZero).isEqualTo("DEPLETED");

        String statusNegative = Budget.deriveStatus(-500L, 500000L, "ACTIVE");
        assertThat(statusNegative).isEqualTo("DEPLETED");
    }

    @Test
    @DisplayName("deriveStatus: ARCHIVED status is preserved regardless of balance")
    void deriveStatus_archived_preservesArchived() {
        String status = Budget.deriveStatus(500000L, 500000L, "ARCHIVED");
        assertThat(status).isEqualTo("ARCHIVED");

        String statusZero = Budget.deriveStatus(0L, 500000L, "ARCHIVED");
        assertThat(statusZero).isEqualTo("ARCHIVED");
    }

    @Test
    @DisplayName("deriveStatus: PLANNED status is preserved when zero initial funds allocated")
    void deriveStatus_planned_preservesPlanned() {
        String status = Budget.deriveStatus(0L, 500000L, "PLANNED");
        assertThat(status).isEqualTo("PLANNED");
    }
}

