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
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.house.expensetracker.dto.CreateHouseholdRequest;
import com.house.expensetracker.dto.HouseholdDto;
import com.house.expensetracker.dto.HouseholdInvitationDto;
import com.house.expensetracker.dto.HouseholdMemberDto;
import com.house.expensetracker.dto.InviteCoOwnerRequest;
import com.house.expensetracker.dto.UpdateHouseholdRequest;
import com.house.expensetracker.exception.ForbiddenException;
import com.house.expensetracker.exception.GlobalExceptionHandler;
import com.house.expensetracker.exception.ResourceNotFoundException;
import com.house.expensetracker.security.AuthenticatedUser;
import com.house.expensetracker.service.HouseholdInvitationService;
import com.house.expensetracker.service.HouseholdService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class HouseholdControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private HouseholdService householdService;

    @Mock
    private HouseholdInvitationService invitationService;

    @BeforeEach
    void setUp() {
        HouseholdController controller = new HouseholdController(householdService, invitationService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/households with valid body returns 201 Created and HouseholdDto")
    void createHousehold_validPayload_returns201() throws Exception {
        CreateHouseholdRequest request = CreateHouseholdRequest.builder()
                .name("Mendoza Family")
                .baseCurrencies(List.of("PEN", "USD"))
                .partnerEmail("maria@example.com")
                .build();

        HouseholdDto expectedDto = HouseholdDto.builder()
                .id("hh_123")
                .name("Mendoza Family")
                .status("ACTIVE")
                .baseCurrencies(List.of("PEN", "USD"))
                .balancesByCurrency(Map.of("PEN", 0L, "USD", 0L))
                .ownerId("usr_carlos_123")
                .members(Map.of("usr_carlos_123", HouseholdMemberDto.builder()
                        .userId("usr_carlos_123")
                        .role("PRIMARY_OWNER")
                        .displayName("Carlos")
                        .email("carlos@example.com")
                        .status("ACTIVE")
                        .build()))
                .memberIds(List.of("usr_carlos_123"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(householdService.createHousehold(
                any(CreateHouseholdRequest.class),
                eq("usr_carlos_123"),
                eq("carlos@example.com"),
                eq("Carlos")))
                .willReturn(expectedDto);

        mockMvc.perform(post("/api/v1/households")
                .principal(new AuthenticatedUser("usr_carlos_123", "carlos@example.com", "Carlos"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("hh_123"))
                .andExpect(jsonPath("$.name").value("Mendoza Family"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.baseCurrencies[0]").value("PEN"))
                .andExpect(jsonPath("$.ownerId").value("usr_carlos_123"));

        verify(invitationService).createInvitation(
                eq("hh_123"),
                any(InviteCoOwnerRequest.class),
                eq("usr_carlos_123"),
                eq("Carlos"));
    }

    @Test
    @DisplayName("POST /api/v1/households with blank name returns 400 Bad Request")
    void createHousehold_invalidPayload_returns400() throws Exception {
        CreateHouseholdRequest request = CreateHouseholdRequest.builder()
                .name("")
                .baseCurrencies(List.of())
                .build();

        mockMvc.perform(post("/api/v1/households")
                .principal(() -> "usr_carlos_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.baseCurrencies").exists());
    }

    @Test
    @DisplayName("GET /api/v1/households/current returns 200 OK and current household")
    void getCurrentHousehold_exists_returns200() throws Exception {
        HouseholdDto expectedDto = HouseholdDto.builder()
                .id("hh_123")
                .name("Mendoza Family")
                .status("ACTIVE")
                .baseCurrencies(List.of("PEN"))
                .ownerId("usr_carlos_123")
                .memberIds(List.of("usr_carlos_123"))
                .build();

        given(householdService.getCurrentHousehold("usr_carlos_123")).willReturn(expectedDto);

        mockMvc.perform(get("/api/v1/households/current")
                .principal(() -> "usr_carlos_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("hh_123"))
                .andExpect(jsonPath("$.name").value("Mendoza Family"));
    }

    @Test
    @DisplayName("GET /api/v1/households/current returns 404 when not found")
    void getCurrentHousehold_notFound_returns404() throws Exception {
        given(householdService.getCurrentHousehold("usr_carlos_123"))
                .willThrow(new ResourceNotFoundException("No active household found"));

        mockMvc.perform(get("/api/v1/households/current")
                .principal(() -> "usr_carlos_123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/v1/households/{id} with valid body returns 200 OK")
    void updateHousehold_authorized_returns200() throws Exception {
        UpdateHouseholdRequest request = UpdateHouseholdRequest.builder()
                .name("Mendoza-Perez Family")
                .baseCurrencies(List.of("USD", "EUR"))
                .build();

        HouseholdDto updatedDto = HouseholdDto.builder()
                .id("hh_123")
                .name("Mendoza-Perez Family")
                .status("ACTIVE")
                .baseCurrencies(List.of("USD", "EUR"))
                .ownerId("usr_carlos_123")
                .build();

        given(householdService.updateHousehold(eq("hh_123"), any(UpdateHouseholdRequest.class), eq("usr_carlos_123")))
                .willReturn(updatedDto);

        mockMvc.perform(patch("/api/v1/households/hh_123")
                .principal(() -> "usr_carlos_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mendoza-Perez Family"));
    }

    @Test
    @DisplayName("PATCH /api/v1/households/{id} forbidden for non-member returns 403")
    void updateHousehold_forbidden_returns403() throws Exception {
        UpdateHouseholdRequest request = UpdateHouseholdRequest.builder()
                .name("Hijacked Household")
                .build();

        given(householdService.updateHousehold(eq("hh_123"), any(UpdateHouseholdRequest.class), eq("usr_stranger_999")))
                .willThrow(new ForbiddenException("Caller is not an authorized member"));

        mockMvc.perform(patch("/api/v1/households/hh_123")
                .principal(() -> "usr_stranger_999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/v1/households/{id}/invitations returns 201 Created")
    void inviteCoOwner_valid_returns201() throws Exception {
        InviteCoOwnerRequest request = InviteCoOwnerRequest.builder()
                .invitedEmail("maria@example.com")
                .build();

        HouseholdInvitationDto invitation = HouseholdInvitationDto.builder()
                .id("inv_1")
                .householdId("hh_123")
                .invitedEmail("maria@example.com")
                .status("PENDING")
                .build();

        given(invitationService.createInvitation(eq("hh_123"), any(InviteCoOwnerRequest.class), eq("usr_carlos_123"), any()))
                .willReturn(invitation);

        mockMvc.perform(post("/api/v1/households/hh_123/invitations")
                .principal(() -> "usr_carlos_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("inv_1"))
                .andExpect(jsonPath("$.invitedEmail").value("maria@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("DELETE /api/v1/households/{id}/invitations/{invitationId} returns 204 No Content")
    void cancelInvitation_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/households/hh_123/invitations/inv_1")
                .principal(() -> "usr_carlos_123"))
                .andExpect(status().isNoContent());

        verify(invitationService).cancelInvitation("hh_123", "inv_1", "usr_carlos_123");
    }
}
