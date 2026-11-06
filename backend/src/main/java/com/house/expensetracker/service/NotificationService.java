package com.house.expensetracker.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.house.expensetracker.dto.NotificationDto;
import com.house.expensetracker.dto.NotificationFeedResponse;
import com.house.expensetracker.exception.BadRequestException;
import com.house.expensetracker.exception.ForbiddenException;
import com.house.expensetracker.exception.ResourceNotFoundException;
import com.house.expensetracker.model.Notification;

@Service
public class NotificationService {

    public static final String COLLECTION_NAME = "notifications";

    private final Firestore firestore;

    @Autowired
    public NotificationService(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference getCollection() {
        return firestore.collection(COLLECTION_NAME);
    }

    public NotificationDto createNotification(Notification n) {
        DocumentReference docRef = getCollection().document();
        Timestamp now = Timestamp.now();
        Instant expiryInstant = Instant.now().plus(Duration.ofDays(90));
        Timestamp expiresAt = Timestamp.ofTimeSecondsAndNanos(expiryInstant.getEpochSecond(), expiryInstant.getNano());

        n.setId(docRef.getId());
        n.setCreatedAt(now);
        n.setExpiresAt(expiresAt);

        try {
            docRef.set(toFirestoreMap(n)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while creating notification", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to create notification: " + e.getMessage(), e);
        }

        return toDto(n);
    }

    public NotificationFeedResponse getNotificationFeed(String callerUid, int limit, String cursor) {
        if (callerUid == null || callerUid.isBlank()) {
            throw new BadRequestException("Caller UID is required");
        }

        int queryLimit = limit > 0 ? limit : 20;
        List<NotificationDto> items = new ArrayList<>();

        try {
            Query query = getCollection()
                    .whereEqualTo("recipientId", callerUid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(queryLimit);

            QuerySnapshot snapshot = query.get().get();
            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                items.add(toDto(fromSnapshot(doc)));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching notification feed", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to fetch notification feed: " + e.getMessage(), e);
        }

        return NotificationFeedResponse.builder()
                .items(items)
                .nextCursor(null)
                .build();
    }

    public Map<String, Object> markAsRead(String notificationId, String callerUid) {
        DocumentReference docRef = getCollection().document(notificationId);
        Notification n = getNotificationEntity(notificationId);

        if (!callerUid.equals(n.getRecipientId())) {
            throw new ForbiddenException("Caller does not own this notification");
        }

        try {
            docRef.update("isRead", true).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while marking notification as read", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to mark notification as read: " + e.getMessage(), e);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("id", notificationId);
        res.put("isRead", true);
        return res;
    }

    public Map<String, Object> markAllAsRead(String callerUid) {
        if (callerUid == null || callerUid.isBlank()) {
            throw new BadRequestException("Caller UID is required");
        }

        int updatedCount = 0;
        try {
            QuerySnapshot snapshot = getCollection()
                    .whereEqualTo("recipientId", callerUid)
                    .whereEqualTo("isRead", false)
                    .get().get();

            if (!snapshot.isEmpty()) {
                WriteBatch batch = firestore.batch();
                for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                    batch.update(doc.getReference(), "isRead", true);
                    updatedCount++;
                }
                batch.commit().get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while marking all as read", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to mark all notifications as read: " + e.getMessage(), e);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("updatedCount", updatedCount);
        return res;
    }

    public Notification getNotificationEntity(String notificationId) {
        try {
            DocumentSnapshot doc = getCollection().document(notificationId).get().get();
            if (!doc.exists()) {
                throw new ResourceNotFoundException("Notification not found: " + notificationId);
            }
            return fromSnapshot(doc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching notification", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to fetch notification: " + e.getMessage(), e);
        }
    }

    public NotificationDto toDto(Notification n) {
        if (n == null) {
            return null;
        }
        Instant created = n.getCreatedAt() != null
                ? Instant.ofEpochSecond(n.getCreatedAt().getSeconds(), n.getCreatedAt().getNanos())
                : Instant.now();
        Instant expires = n.getExpiresAt() != null
                ? Instant.ofEpochSecond(n.getExpiresAt().getSeconds(), n.getExpiresAt().getNanos())
                : null;

        return NotificationDto.builder()
                .id(n.getId())
                .householdId(n.getHouseholdId())
                .recipientId(n.getRecipientId())
                .actorId(n.getActorId())
                .budgetId(n.getBudgetId())
                .budgetTitle(n.getBudgetTitle())
                .invitationId(n.getInvitationId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.isRead())
                .expiresAt(expires)
                .createdAt(created)
                .build();
    }

    private Map<String, Object> toFirestoreMap(Notification n) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", n.getId());
        map.put("householdId", n.getHouseholdId());
        map.put("recipientId", n.getRecipientId());
        map.put("actorId", n.getActorId());
        map.put("budgetId", n.getBudgetId());
        map.put("budgetTitle", n.getBudgetTitle());
        map.put("invitationId", n.getInvitationId());
        map.put("type", n.getType());
        map.put("title", n.getTitle());
        map.put("message", n.getMessage());
        map.put("isRead", n.isRead());
        map.put("expiresAt", n.getExpiresAt());
        map.put("createdAt", n.getCreatedAt());
        return map;
    }

    private Notification fromSnapshot(DocumentSnapshot doc) {
        Notification n = new Notification();
        n.setId(doc.getId());
        n.setHouseholdId(doc.getString("householdId"));
        n.setRecipientId(doc.getString("recipientId"));
        n.setActorId(doc.getString("actorId"));
        n.setBudgetId(doc.getString("budgetId"));
        n.setBudgetTitle(doc.getString("budgetTitle"));
        n.setInvitationId(doc.getString("invitationId"));
        n.setType(doc.getString("type"));
        n.setTitle(doc.getString("title"));
        n.setMessage(doc.getString("message"));

        Boolean read = doc.getBoolean("isRead");
        n.setRead(read != null && read);

        n.setExpiresAt(doc.getTimestamp("expiresAt"));
        n.setCreatedAt(doc.getTimestamp("createdAt"));
        return n;
    }
}

