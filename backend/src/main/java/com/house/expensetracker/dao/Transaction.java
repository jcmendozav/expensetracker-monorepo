package com.house.expensetracker.dao; // <--- MATCH THIS TO YOUR FOLDER!

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.Exclude;

import lombok.AllArgsConstructor;
import lombok.Builder; // <--- Add this
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // <--- Generates all Getters/Setters (including setId)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private String id;
    private String userId;

    // 🔴 Internal Google storage format (Hidden from JSON)
    // @Data will generate getTransactionDate() and setTransactionDate()
    // which is good for Java, but we hide it from JSON.
    @JsonIgnore
    private Timestamp transactionDate;

    private String description;
    private Double originalAmount;
    private String originalCurrency;
    private Double baseAmount;
    private Double baseCurrency;
    private Long householdId;
    private Long categoryId;

    // --- CUSTOM DATE HANDLING ---

    // 1. Sends Milliseconds to Angular
    @JsonProperty("transactionDate")
    @Exclude
    public Long getTransactionDateAsLong() {
        return transactionDate != null ? transactionDate.toDate().getTime() : null;
    }

    // 2. Receives Milliseconds from Angular
    @JsonProperty("transactionDate")
    public void setTransactionDateFromLong(Long millis) {
        if (millis != null) {
            this.transactionDate = Timestamp.of(new Date(millis));
        }
    }
}