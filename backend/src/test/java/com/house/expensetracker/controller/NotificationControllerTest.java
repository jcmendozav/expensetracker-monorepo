package com.house.expensetracker.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.house.expensetracker.dto.NotificationDto;
import com.house.expensetracker.dto.NotificationFeedResponse;
import com.house.expensetracker.exception.GlobalExceptionHandler;
import com.house.expensetracker.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        NotificationController controller = new NotificationController(notificationService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/notifications returns 200 OK and notification feed")
    void getFeed_returns200() throws Exception {
        NotificationDto item = NotificationDto.builder()
                .id("ntf_1")
                .householdId("hh_123")
                .recipientId("usr_carlos_123")
                .actorId("usr_maria_456")
                .type("EXPENSE_LOGGED")
                .title("Expense Logged")
                .message("Maria logged 250.00 on Kitchen Renovation")
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        NotificationFeedResponse response = NotificationFeedResponse.builder()
                .items(List.of(item))
                .nextCursor(null)
                .build();

        given(notificationService.getNotificationFeed("usr_carlos_123", 20, null))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/notifications")
                .principal(() -> "usr_carlos_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("ntf_1"))
                .andExpect(jsonPath("$.items[0].type").value("EXPENSE_LOGGED"))
                .andExpect(jsonPath("$.items[0].message").value("Maria logged 250.00 on Kitchen Renovation"));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/{id}/read marks alert read and returns 200 OK")
    void markAsRead_returns200() throws Exception {
        given(notificationService.markAsRead("ntf_1", "usr_carlos_123"))
                .willReturn(Map.of("id", "ntf_1", "isRead", true));

        mockMvc.perform(patch("/api/v1/notifications/ntf_1/read")
                .principal(() -> "usr_carlos_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ntf_1"))
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/notifications/mark-all-read returns 200 OK and updatedCount")
    void markAllAsRead_returns200() throws Exception {
        given(notificationService.markAllAsRead("usr_carlos_123"))
                .willReturn(Map.of("updatedCount", 5));

        mockMvc.perform(post("/api/v1/notifications/mark-all-read")
                .principal(() -> "usr_carlos_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(5));
    }
}

