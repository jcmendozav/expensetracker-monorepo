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
import com.house.expensetracker.dto.HouseholdDto;
import com.house.expensetracker.dto.HouseholdInvitationDto;
import com.house.expensetracker.dto.InviteCoOwnerRequest;
import com.house.expensetracker.exception.BadRequestException;
import com.house.expensetracker.exception.ForbiddenException;
import com.house.expensetracker.model.Household;
import com.house.expensetracker.model.HouseholdInvitation;
import com.house.expensetracker.model.HouseholdMember;

@ExtendWith(MockitoExtension.class)
class HouseholdInvitationServiceTest {

    @Mock
    private Firestore firestore;

    @Mock
    private HouseholdService householdService;

    @Mock
    private CollectionReference invitationCollection;

    @Mock
    private CollectionReference householdCollection;

    @Mock
    private DocumentReference invitationDocRef;

    @Mock
    private DocumentReference householdDocRef;

    @Mock
    private WriteBatch writeBatch;

    private HouseholdInvitationService invitationService;

    @BeforeEach
    void setUp() {
        invitationService = spy(new HouseholdInvitationService(firestore, householdService));
    }

    private void setupInvitationCollection() {
        given(firestore.collection(HouseholdInvitationService.COLLECTION_NAME)).willReturn(invitationCollection);
    }

    @Test
    @DisplayName("createInvitation saves invitation and returns DTO when valid")
    void createInvitation_valid_savesAndReturnsDto() {
        String householdId = "hh_123";
        Household household = Household.builder()
                .id(householdId)
                .name("Mendoza Family")
                .ownerId("usr_carlos_123")
                .memberIds(List.of("usr_carlos_123"))
                .members(Map.of("usr_carlos_123", HouseholdMember.builder()
                        .userId("usr_carlos_123")
                        .email("carlos@example.com")
                        .build()))
                .build();

        given(householdService.getHouseholdEntity(householdId)).willReturn(household);
        setupInvitationCollection();
        given(invitationCollection.document()).willReturn(invitationDocRef);
        given(invitationDocRef.getId()).willReturn("inv_new");
        given(invitationDocRef.set(anyMap())).willReturn(ApiFutures.immediateFuture(null));

        InviteCoOwnerRequest request = InviteCoOwnerRequest.builder()
                .invitedEmail("maria@example.com")
                .build();

        HouseholdInvitationDto dto = invitationService.createInvitation(householdId, request, "usr_carlos_123", "Carlos Mendoza");

        assertThat(dto.getId()).isEqualTo("inv_new");
        assertThat(dto.getHouseholdId()).isEqualTo(householdId);
        assertThat(dto.getInvitedEmail()).isEqualTo("maria@example.com");
        assertThat(dto.getStatus()).isEqualTo("PENDING");
        verify(invitationDocRef).set(anyMap());
    }

    @Test
    @DisplayName("createInvitation throws BadRequestException when user is already a member")
    void createInvitation_alreadyMember_throwsBadRequestException() {
        String householdId = "hh_123";
        Household household = Household.builder()
                .id(householdId)
                .name("Mendoza Family")
                .memberIds(List.of("usr_carlos_123", "usr_maria_456"))
                .members(Map.of("usr_maria_456", HouseholdMember.builder()
                        .userId("usr_maria_456")
                        .email("maria@example.com")
                        .build()))
                .build();

        given(householdService.getHouseholdEntity(householdId)).willReturn(household);

        InviteCoOwnerRequest request = InviteCoOwnerRequest.builder()
                .invitedEmail("maria@example.com")
                .build();

        assertThatThrownBy(() -> invitationService.createInvitation(householdId, request, "usr_carlos_123", "Carlos"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already a household member");
    }

    @Test
    @DisplayName("acceptInvitation executes atomic batch adding member and updating invite status")
    void acceptInvitation_valid_executesAtomicBatch() {
        String invitationId = "inv_01";
        String householdId = "hh_123";

        HouseholdInvitation invitation = HouseholdInvitation.builder()
                .id(invitationId)
                .householdId(householdId)
                .invitedEmail("maria@example.com")
                .status("PENDING")
                .createdAt(Timestamp.now())
                .build();

        Household household = Household.builder()
                .id(householdId)
                .name("Mendoza Family")
                .ownerId("usr_carlos_123")
                .memberIds(new java.util.ArrayList<>(List.of("usr_carlos_123")))
                .members(new HashMap<>(Map.of("usr_carlos_123", HouseholdMember.builder()
                        .userId("usr_carlos_123")
                        .email("carlos@example.com")
                        .build())))
                .build();

        doReturn(invitation).when(invitationService).getInvitationEntity(invitationId);
        given(householdService.getHouseholdEntity(householdId)).willReturn(household);

        setupInvitationCollection();
        given(invitationCollection.document(invitationId)).willReturn(invitationDocRef);
        given(firestore.collection(HouseholdService.COLLECTION_NAME)).willReturn(householdCollection);
        given(householdCollection.document(householdId)).willReturn(householdDocRef);

        given(firestore.batch()).willReturn(writeBatch);
        given(writeBatch.commit()).willReturn(ApiFutures.immediateFuture(List.<WriteResult>of()));

        HouseholdDto expectedDto = HouseholdDto.builder().id(householdId).build();
        given(householdService.toDto(any(Household.class))).willReturn(expectedDto);

        HouseholdDto result = invitationService.acceptInvitation(invitationId, "usr_maria_456", "maria@example.com", "Maria Mendoza");

        assertThat(result.getId()).isEqualTo(householdId);
        verify(writeBatch).update(eq(invitationDocRef), eq("status"), eq("ACCEPTED"), eq("updatedAt"), any());
        verify(writeBatch).update(eq(householdDocRef), eq("memberIds"), any(), eq("members"), anyMap(), eq("updatedAt"), any());
        verify(writeBatch).commit();
    }

    @Test
    @DisplayName("acceptInvitation throws ForbiddenException when caller email does not match invited email")
    void acceptInvitation_emailMismatch_throwsForbiddenException() {
        String invitationId = "inv_01";
        HouseholdInvitation invitation = HouseholdInvitation.builder()
                .id(invitationId)
                .householdId("hh_123")
                .invitedEmail("maria@example.com")
                .status("PENDING")
                .createdAt(Timestamp.now())
                .build();

        doReturn(invitation).when(invitationService).getInvitationEntity(invitationId);

        assertThatThrownBy(() -> invitationService.acceptInvitation(invitationId, "usr_attacker", "attacker@example.com", "Attacker"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("sent to a different email address");
    }

    @Test
    @DisplayName("acceptInvitation throws BadRequestException when invitation has expired (past 7 days)")
    void acceptInvitation_expired_throwsBadRequestException() {
        String invitationId = "inv_01";
        // 8 days ago
        long eightDaysAgoSeconds = Timestamp.now().getSeconds() - (8L * 24 * 60 * 60);
        HouseholdInvitation invitation = HouseholdInvitation.builder()
                .id(invitationId)
                .householdId("hh_123")
                .invitedEmail("maria@example.com")
                .status("PENDING")
                .createdAt(Timestamp.ofTimeSecondsAndNanos(eightDaysAgoSeconds, 0))
                .build();

        doReturn(invitation).when(invitationService).getInvitationEntity(invitationId);
        setupInvitationCollection();
        given(invitationCollection.document(invitationId)).willReturn(invitationDocRef);
        given(invitationDocRef.update(eq("status"), eq("EXPIRED"), eq("updatedAt"), any()))
                .willReturn(ApiFutures.immediateFuture(null));

        assertThatThrownBy(() -> invitationService.acceptInvitation(invitationId, "usr_maria_456", "maria@example.com", "Maria"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("declineInvitation updates status to DECLINED")
    void declineInvitation_updatesStatus() {
        String invitationId = "inv_01";
        HouseholdInvitation invitation = HouseholdInvitation.builder()
                .id(invitationId)
                .householdId("hh_123")
                .invitedEmail("maria@example.com")
                .status("PENDING")
                .build();

        doReturn(invitation).when(invitationService).getInvitationEntity(invitationId);
        setupInvitationCollection();
        given(invitationCollection.document(invitationId)).willReturn(invitationDocRef);
        given(invitationDocRef.update(eq("status"), eq("DECLINED"), eq("updatedAt"), any()))
                .willReturn(ApiFutures.immediateFuture(null));

        Map<String, Object> result = invitationService.declineInvitation(invitationId, "usr_maria_456", "maria@example.com");

        assertThat(result.get("status")).isEqualTo("DECLINED");
    }

    @Test
    @DisplayName("cancelInvitation updates status to CANCELLED when invoked by primary owner")
    void cancelInvitation_byOwner_updatesStatusToCancelled() {
        String invitationId = "inv_01";
        String householdId = "hh_123";

        Household household = Household.builder()
                .id(householdId)
                .ownerId("usr_carlos_123")
                .memberIds(List.of("usr_carlos_123"))
                .build();

        HouseholdInvitation invitation = HouseholdInvitation.builder()
                .id(invitationId)
                .householdId(householdId)
                .status("PENDING")
                .build();

        given(householdService.getHouseholdEntity(householdId)).willReturn(household);
        doReturn(invitation).when(invitationService).getInvitationEntity(invitationId);

        setupInvitationCollection();
        given(invitationCollection.document(invitationId)).willReturn(invitationDocRef);
        given(invitationDocRef.update(eq("status"), eq("CANCELLED"), eq("updatedAt"), any()))
                .willReturn(ApiFutures.immediateFuture(null));

        invitationService.cancelInvitation(householdId, invitationId, "usr_carlos_123");

        verify(householdService).assertOwner(household, "usr_carlos_123");
        verify(invitationDocRef).update(eq("status"), eq("CANCELLED"), eq("updatedAt"), any());
    }

    @Test
    @DisplayName("isExpired correctly evaluates 7 day boundary")
    void isExpired_evaluatesCorrectly() {
        HouseholdInvitation fresh = HouseholdInvitation.builder()
                .createdAt(Timestamp.now())
                .build();
        assertThat(invitationService.isExpired(fresh)).isFalse();

        long sixDaysAgo = Timestamp.now().getSeconds() - (6L * 24 * 60 * 60);
        HouseholdInvitation sixDays = HouseholdInvitation.builder()
                .createdAt(Timestamp.ofTimeSecondsAndNanos(sixDaysAgo, 0))
                .build();
        assertThat(invitationService.isExpired(sixDays)).isFalse();

        long eightDaysAgo = Timestamp.now().getSeconds() - (8L * 24 * 60 * 60);
        HouseholdInvitation eightDays = HouseholdInvitation.builder()
                .createdAt(Timestamp.ofTimeSecondsAndNanos(eightDaysAgo, 0))
                .build();
        assertThat(invitationService.isExpired(eightDays)).isTrue();
    }
}
