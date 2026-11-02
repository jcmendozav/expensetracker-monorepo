# Backend Guidelines: Expense Tracker (`/backend`)

> Spring Boot REST API & Google Cloud Firestore service.

## 1. Design & Architecture Principles

- **Design Philosophy:** Adhere strictly to [docs/DESIGN_PHILOSOPHY.md](../docs/DESIGN_PHILOSOPHY.md).
  - Create **deep domain services** that encapsulate business rules and Firestore interactions.
  - Avoid shallow pass-through services or anemic data models.
  - Define errors out of existence: handle idempotent operations and boundary conditions gracefully.
- **DTO Separation:** Never expose raw Firestore entities directly in REST controllers. All incoming requests and outgoing responses must use explicit DTO records/classes.
- **Transactions & Concurrency:** Multi-document ledger mutations (e.g. transfers across budgets, voids with refunds) must execute atomically inside Firestore transaction batches.

---

## 2. Invariants & Standards

- **Monetary Values:** Store and transmit all money amounts as integer minor units (`Long cents`). Never use `double` or `float` for currency.
- **API Routing:** All REST endpoints must start with `/api/v1/`.
- **Authentication:** Validate Firebase ID tokens on all `/api/v1/*` endpoints (`Bearer <token>`). Extract the authenticated `uid` from the security context; do not rely on client-supplied user IDs.
- **Validation:** Use Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Min`, `@Size`) on controller DTO inputs.

---

## 3. Test Strategy & Architecture

All backend modules must follow a 4-tier testing pyramid grounded in [docs/DESIGN_PHILOSOPHY.md](../docs/DESIGN_PHILOSOPHY.md) (testing deep contracts, defining errors out of existence, and avoiding brittle mock chains of third-party SDKs):

- **Layer 1: Pure Domain & Calculation Unit Tests (JUnit 5 + AssertJ)**
  - Zero Spring context; fast, sub-second execution.
  - Test pure business logic: monetary minor-unit calculations (`long cents`), currency rounding, domain entity state transitions, and soft-void/refund arithmetic.
- **Layer 2: REST Controller Slice Tests (`@WebMvcTest`)**
  - Use `MockMvc` with `@MockBean` service dependencies.
  - Verify `/api/v1/...` routes, Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Min`), HTTP response status codes (`201`, `200`, `400`, `403`), and DTO JSON serialization.
- **Layer 3: Security & Filter Tests**
  - Verify `FirebaseTokenFilter` and `SecurityConfig`: missing or expired tokens return `401 Unauthorized`, valid tokens properly populate `SecurityContextHolder` with the authenticated `uid`, and CORS headers are asserted.
- **Layer 4: Deep Service & Firestore Concurrency Integration Tests**
  - Run against the **Firebase Local Firestore Emulator** (`localhost:8085`).
  - Test multi-document atomic batch operations, transaction rollbacks on failure, idempotency (e.g. repeated soft-voids), and non-blocking event publishing.

---

## 4. Deterministic Verification Commands

Execute from the `/backend` directory:

```bash
# Run unit & integration tests
./gradlew test

# Run a specific test class
./gradlew test --tests "*HouseholdControllerTest*"

# Start local Spring Boot server (port 8080)
./gradlew bootRun

# Check build without running tests
./gradlew build -x test
```
