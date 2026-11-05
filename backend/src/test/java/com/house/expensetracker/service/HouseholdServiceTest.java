package com.house.expensetracker.service;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.house.expensetracker.dto.HouseholdDto;
import com.house.expensetracker.exception.BadRequestException;
import com.house.expensetracker.exception.ForbiddenException;
import com.house.expensetracker.model.Household;
import com.house.expensetracker.model.HouseholdMember;

@ExtendWith(MockitoExtension.class)
class HouseholdServiceTest {

    @Mock
    private Firestore firestore;

    private HouseholdService householdService;

    @BeforeEach
    void setUp() {
        householdService = new HouseholdService(firestore);
    }

    @Test
    @DisplayName("assertMembership passes when caller is in memberIds")
    void assertMembership_authorizedMember_passes() {
        Household household = Household.builder()
                .id("hh_1")
                .memberIds(List.of("user_1", "user_2"))
                .build();

        householdService.assertMembership(household, "user_1");
        householdService.assertMembership(household, "user_2");
    }

    @Test
    @DisplayName("assertMembership throws ForbiddenException when caller is not in memberIds")
    void assertMembership_unauthorizedMember_throwsForbidden() {
        Household household = Household.builder()
                .id("hh_1")
                .memberIds(List.of("user_1"))
                .build();

        assertThatThrownBy(() -> householdService.assertMembership(household, "intruder"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not an authorized member");
    }

    @Test
    @DisplayName("toDto properly maps Household and members")
    void toDto_mapsAllFieldsCorrectly() {
        Household household = Household.builder()
                .id("hh_123")
                .name("Mendoza Family")
                .status("ACTIVE")
                .baseCurrencies(List.of("PEN", "USD"))
                .balancesByCurrency(Map.of("PEN", 10000L, "USD", 5000L))
                .ownerId("user_1")
                .memberIds(List.of("user_1"))
                .members(Map.of("user_1", HouseholdMember.builder()
                        .userId("user_1")
                        .role("PRIMARY_OWNER")
                        .displayName("Carlos")
                        .email("carlos@example.com")
                        .status("ACTIVE")
                        .build()))
                .createdAt(Timestamp.now())
                .updatedAt(Timestamp.now())
                .build();

        HouseholdDto dto = householdService.toDto(household);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("hh_123");
        assertThat(dto.getName()).isEqualTo("Mendoza Family");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
        assertThat(dto.getBaseCurrencies()).containsExactly("PEN", "USD");
        assertThat(dto.getBalancesByCurrency()).containsEntry("PEN", 10000L).containsEntry("USD", 5000L);
        assertThat(dto.getMembers()).containsKey("user_1");
        assertThat(dto.getMembers().get("user_1").getRole()).isEqualTo("PRIMARY_OWNER");
    }

    @Test
    @DisplayName("getCurrentHousehold throws BadRequestException when UID is null or empty")
    void getCurrentHousehold_invalidUid_throwsBadRequest() {
        assertThatThrownBy(() -> householdService.getCurrentHousehold(null))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> householdService.getCurrentHousehold("   "))
                .isInstanceOf(BadRequestException.class);
    }
}

