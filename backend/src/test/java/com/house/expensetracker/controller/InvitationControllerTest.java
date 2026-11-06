package com.house.expensetracker.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.house.expensetracker.dto.HouseholdDto;
import com.house.expensetracker.dto.HouseholdInvitationDto;
import com.house.expensetracker.exception.GlobalExceptionHandler;
import com.house.expensetracker.service.HouseholdInvitationService;

@ExtendWith(MockitoExtension.class)
class InvitationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HouseholdInvitationService invitationService;

    @BeforeEach
    void setUp() {
        InvitationController controller = new InvitationController(invitationService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/invitations/pending returns 200 OK and pending invitations")
    void getPendingInvitations_returns200() throws Exception {
        HouseholdInvitationDto inv = HouseholdInvitationDto.builder()
                .id("inv_1")
                .householdId("hh_123")
                .householdName("Mendoza Family")
                .invitedEmail("maria@example.com")
                .status("PENDING")
                .build();

        given(invitationService.getPendingInvitationsForEmail("maria@example.com"))
                .willReturn(List.of(inv));

        mockMvc.perform(get("/api/v1/invitations/pending")
                .param("email", "maria@example.com")
                .principal(() -> "usr_maria_456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("inv_1"))
                .andExpect(jsonPath("$[0].householdName").value("Mendoza Family"));
    }

    @Test
    @DisplayName("POST /api/v1/invitations/{id}/accept returns 200 OK and updated HouseholdDto")
    void acceptInvitation_returns200() throws Exception {
        HouseholdDto accepted = HouseholdDto.builder()
                .id("hh_123")
                .name("Mendoza Family")
                .memberIds(List.of("usr_carlos_123", "usr_maria_456"))
                .build();

        given(invitationService.acceptInvitation(eq("inv_1"), eq("usr_maria_456"), any(), any()))
                .willReturn(accepted);

        mockMvc.perform(post("/api/v1/invitations/inv_1/accept")
                .principal(() -> "usr_maria_456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("hh_123"))
                .andExpect(jsonPath("$.memberIds[1]").value("usr_maria_456"));
    }

    @Test
    @DisplayName("POST /api/v1/invitations/{id}/decline returns 200 OK and DECLINED status")
    void declineInvitation_returns200() throws Exception {
        given(invitationService.declineInvitation("inv_1", "usr_maria_456"))
                .willReturn(Map.of("id", "inv_1", "status", "DECLINED"));

        mockMvc.perform(post("/api/v1/invitations/inv_1/decline")
                .principal(() -> "usr_maria_456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }
}

