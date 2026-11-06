package com.house.expensetracker.service;

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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.core.ApiFutures;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import com.house.expensetracker.dto.NotificationDto;
import com.house.expensetracker.dto.NotificationFeedResponse;
import com.house.expensetracker.exception.BadRequestException;
import com.house.expensetracker.exception.ForbiddenException;
import com.house.expensetracker.model.Notification;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private Firestore firestore;

    @Mock
    private CollectionReference notificationCollection;

    @Mock
    private DocumentReference docRef;

    @Mock
    private DocumentSnapshot docSnapshot;

    @Mock
    private Query query;

    @Mock
    private QuerySnapshot querySnapshot;

    @Mock
    private QueryDocumentSnapshot queryDoc1;

    @Mock
    private QueryDocumentSnapshot queryDoc2;

    @Mock
    private WriteBatch writeBatch;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = spy(new NotificationService(firestore));
    }

    private void setupCollection() {
        given(firestore.collection(NotificationService.COLLECTION_NAME)).willReturn(notificationCollection);
    }

    @Test
    @DisplayName("createNotification sets 90-day TTL expiresAt, commits and returns DTO")
    void createNotification_setsTTLAndReturnsDto() {
        setupCollection();
        given(notificationCollection.document()).willReturn(docRef);
        given(docRef.getId()).willReturn("ntf_123");
        given(docRef.set(anyMap())).willReturn(ApiFutures.immediateFuture(null));

        Notification notification = Notification.builder()
                .householdId("hh_1")
                .recipientId("usr_maria")
                .actorId("usr_carlos")
                .type("EXPENSE_LOGGED")
                .title("Expense Logged")
                .message("Carlos logged 150.00")
                .isRead(false)
                .build();

        NotificationDto dto = notificationService.createNotification(notification);

        assertThat(dto.getId()).isEqualTo("ntf_123");
        assertThat(dto.getExpiresAt()).isNotNull();
        // 90 days in the future should be roughly now + 90 days
        assertThat(dto.getExpiresAt().getEpochSecond()).isGreaterThan(dto.getCreatedAt().getEpochSecond() + (89L * 86400));
        verify(docRef).set(anyMap());
    }

    @Test
    @DisplayName("getNotificationFeed returns items and sets nextCursor when more results exist")
    void getNotificationFeed_paginationWithNextCursor() {
        setupCollection();
        given(notificationCollection.whereEqualTo("recipientId", "usr_maria")).willReturn(query);
        given(query.orderBy("createdAt", Query.Direction.DESCENDING)).willReturn(query);
        // limit = 1, so queryLimit + 1 = 2
        given(query.limit(2)).willReturn(query);
        given(query.get()).willReturn(ApiFutures.immediateFuture(querySnapshot));

        given(querySnapshot.getDocuments()).willReturn(List.of(queryDoc1, queryDoc2));

        Notification n1 = Notification.builder().id("n1").recipientId("usr_maria").title("First").createdAt(Timestamp.now()).build();
        doReturn(n1).when(notificationService).fromSnapshot(queryDoc1);
        given(queryDoc1.getId()).willReturn("n1");

        NotificationFeedResponse response = notificationService.getNotificationFeed("usr_maria", 1, null);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getId()).isEqualTo("n1");
        assertThat(response.getNextCursor()).isEqualTo("n1");
    }

    @Test
    @DisplayName("getNotificationFeed uses cursor to startAfter existing cursor document")
    void getNotificationFeed_withCursor_startsAfter() {
        setupCollection();
        given(notificationCollection.whereEqualTo("recipientId", "usr_maria")).willReturn(query);
        given(query.orderBy("createdAt", Query.Direction.DESCENDING)).willReturn(query);

        DocumentReference cursorRef = org.mockito.Mockito.mock(DocumentReference.class);
        DocumentSnapshot cursorDoc = org.mockito.Mockito.mock(DocumentSnapshot.class);
        given(notificationCollection.document("cursor_123")).willReturn(cursorRef);
        given(cursorRef.get()).willReturn(ApiFutures.immediateFuture(cursorDoc));
        given(cursorDoc.exists()).willReturn(true);

        given(query.startAfter(cursorDoc)).willReturn(query);
        given(query.limit(21)).willReturn(query);
        given(query.get()).willReturn(ApiFutures.immediateFuture(querySnapshot));
        given(querySnapshot.getDocuments()).willReturn(List.of());

        NotificationFeedResponse response = notificationService.getNotificationFeed("usr_maria", 20, "cursor_123");

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getNextCursor()).isNull();
        verify(query).startAfter(cursorDoc);
    }

    @Test
    @DisplayName("markAsRead updates isRead to true when caller is recipient")
    void markAsRead_validRecipient_updatesRead() {
        setupCollection();
        given(notificationCollection.document("ntf_1")).willReturn(docRef);

        Notification n = Notification.builder()
                .id("ntf_1")
                .recipientId("usr_maria")
                .isRead(false)
                .build();

        doReturn(n).when(notificationService).getNotificationEntity("ntf_1");
        given(docRef.update("isRead", true)).willReturn(ApiFutures.immediateFuture(null));

        Map<String, Object> result = notificationService.markAsRead("ntf_1", "usr_maria");

        assertThat(result.get("isRead")).isEqualTo(true);
        verify(docRef).update("isRead", true);
    }

    @Test
    @DisplayName("markAsRead throws ForbiddenException when caller is not the recipient")
    void markAsRead_notRecipient_throwsForbiddenException() {
        setupCollection();
        given(notificationCollection.document("ntf_1")).willReturn(docRef);

        Notification n = Notification.builder()
                .id("ntf_1")
                .recipientId("usr_maria")
                .build();

        doReturn(n).when(notificationService).getNotificationEntity("ntf_1");

        assertThatThrownBy(() -> notificationService.markAsRead("ntf_1", "usr_attacker"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Caller does not own this notification");
    }

    @Test
    @DisplayName("markAllAsRead marks unread notifications read in WriteBatch")
    void markAllAsRead_commitsBatch() {
        setupCollection();
        given(notificationCollection.whereEqualTo("recipientId", "usr_maria")).willReturn(query);
        given(query.whereEqualTo("isRead", false)).willReturn(query);
        given(query.get()).willReturn(ApiFutures.immediateFuture(querySnapshot));

        given(querySnapshot.isEmpty()).willReturn(false);
        given(querySnapshot.getDocuments()).willReturn(List.of(queryDoc1));
        given(queryDoc1.getReference()).willReturn(docRef);

        given(firestore.batch()).willReturn(writeBatch);
        given(writeBatch.commit()).willReturn(ApiFutures.immediateFuture(List.<WriteResult>of()));

        Map<String, Object> result = notificationService.markAllAsRead("usr_maria");

        assertThat(result.get("updatedCount")).isEqualTo(1);
        verify(writeBatch).update(docRef, "isRead", true);
        verify(writeBatch).commit();
    }
}
