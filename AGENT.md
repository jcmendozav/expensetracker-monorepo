# Expense Tracker Backend — Agent Guide (`AGENT.md`)

This document provides context, architectural guidelines, code conventions, and workflows for AI coding assistants working in this repository.

---

## 1. Project Overview

* **Application Name:** `expensetracker`
* **Package Root:** `com.house.expensetracker`
* **Architecture:** RESTful API Backend integrating **Spring Boot** with **Google Firebase Admin SDK** (Authentication & Cloud Firestore).
* **Frontend Companion:** Angular Client (running by default on `http://localhost:4200`).

---

## 2. Tech Stack & Prerequisites

* **Language:** Java 21 (`JavaLanguageVersion.of(21)`)
* **Framework:** Spring Boot 4.0.0 (`io.spring.dependency-management:1.1.7`)
* **Build System:** Gradle (using Wrapper `9.2.1`)
* **Database & Auth:** Google Cloud Firestore & Firebase Auth via `firebase-admin:9.7.0`
* **Utilities & Testing:** Project Lombok, JUnit 5, Spring Boot Starter Test

---

## 3. Directory & Package Structure

```
expensetracker/
├── build.gradle                                  # Dependency and build definitions
├── settings.gradle                               # Project settings
├── gradlew / gradlew.bat                         # Gradle wrapper
├── gradle/wrapper/gradle-wrapper.properties      # Gradle 9.2.1 distribution
└── src/
    ├── main/
    │   ├── java/com/house/expensetracker/
    │   │   ├── ExpenseTrackerApplication.java    # Application entry point
    │   │   ├── config/
    │   │   │   ├── FirebaseConfig.java           # FirebaseApp, Auth, and Firestore beans
    │   │   │   ├── SecurityConfig.java           # Stateless security filter chain & CORS
    │   │   │   └── WebConfig.java                # Spring Web MVC config
    │   │   ├── controller/
    │   │   │   └── TransactionController.java    # REST API endpoints (/api/transactions)
    │   │   ├── dao/
    │   │   │   └── Transaction.java              # Firestore entity POJO & date mapping
    │   │   ├── dto/
    │   │   │   └── TransactionRequest.java       # Request payload DTOs
    │   │   ├── security/
    │   │   │   └── FirebaseTokenFilter.java      # Bearer token interceptor & validator
    │   │   └── service/
    │   │       └── TransactionService.java       # Firestore business logic & security checks
    │   └── resources/
    │       ├── application.properties            # Spring configuration & service account path
    │       └── house-expense-tracker-admin.json  # Firebase Admin service account key
    └── test/
        └── java/com/house/expensetracker/        # Controller and Service unit tests
```

---

## 4. Key Architectural Patterns & Conventions

### A. Authentication & Authorization
* **Stateless Security:** Spring Security is configured to `SessionCreationPolicy.STATELESS` with CSRF disabled for REST endpoints.
* **Token Verification:** `FirebaseTokenFilter` intercepts all HTTP requests with `Authorization: Bearer <ID_TOKEN>`, verifies the token with `FirebaseAuth`, and injects the Firebase UID into the Spring `SecurityContextHolder`.
* **User Context:** Controllers and Services **must** extract the authenticated user identity via `java.security.Principal` (`principal.getName()`), which contains the verified Firebase UID.

### B. Firestore & Database Access
* **Injected Beans:** Always use the Spring-managed `Firestore` bean (defined in `FirebaseConfig`).
* **Ownership Checks:** Every mutating Firestore operation (`update`, `delete`) **must verify document ownership** against the current user's UID (`userId`) before executing.
* **Date & Timestamp Handling:** 
  * Firestore stores dates internally as `com.google.cloud.Timestamp` (`@JsonIgnore`).
  * The API communicates dates as epoch milliseconds (`Long`) using custom getters/setters (`@JsonProperty("transactionDate")`).

### C. CORS Configuration
* CORS is enabled for `http://localhost:4200` and `http://127.0.0.1:4200` in `SecurityConfig.java` and on Controller `@CrossOrigin` annotations.

---

## 5. Development & Build Commands

```bash
# Build the project
./gradlew build

# Run unit and integration tests
./gradlew test

# Start the Spring Boot development server (Port 8080)
./gradlew bootRun

# Check dependencies
./gradlew dependencies
```

---

## 6. Guidelines for AI Agents Making Changes

1. **Maintain Type Safety & DTO Layer:** Keep controller request bodies decoupled from Firestore POJOs using DTOs (`TransactionRequest`).
2. **Handle Firestore Asynchronous Calls:** Use `ApiFuture<T>` methods and handle `InterruptedException` and `ExecutionException` properly (resetting the thread interrupt flag when catching `InterruptedException`).
3. **Never Hardcode Secrets:** Keep credentials managed through `application.properties` and the injected `service-account-path`.
4. **Preserve Lombok Annotations:** Use `@Data`, `@Builder`, `@NoArgsConstructor`, and `@AllArgsConstructor` consistently across entity and DTO classes.
