package com.house.expensetracker.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.house.expensetracker.dto.NotificationFeedResponse;
import com.house.expensetracker.service.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationFeedResponse> getFeed(
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
            @RequestParam(value = "cursor", required = false) String cursor,
            Principal principal) {
        String callerUid = principal.getName();
        NotificationFeedResponse response = notificationService.getNotificationFeed(callerUid, limit, cursor);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable("id") String id,
            Principal principal) {
        String callerUid = principal.getName();
        Map<String, Object> result = notificationService.markAsRead(id, callerUid);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Principal principal) {
        String callerUid = principal.getName();
        Map<String, Object> result = notificationService.markAllAsRead(callerUid);
        return ResponseEntity.ok(result);
    }
}

