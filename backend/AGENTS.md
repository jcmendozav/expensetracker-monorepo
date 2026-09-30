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

## 3. Deterministic Verification Commands

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
