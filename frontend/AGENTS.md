# Frontend Guidelines: Expense Tracker (`/frontend`)

> Angular SPA & Angular Material 3 client application.

## 1. Design & Architecture Principles

- **Design Philosophy:** Adhere strictly to [docs/DESIGN_PHILOSOPHY.md](file:///Users/jampiermendoza/Projects/expensetracker-monorepo/docs/DESIGN_PHILOSOPHY.md).
  - Create **deep stateful Angular services** that encapsulate HTTP communication, reactive state management, and data transformations.
  - Keep UI components **presentational (dumb)**—focused strictly on template rendering, user event emission, and view concerns.
- **API Contracts:** TypeScript interfaces must strictly match the Spring Boot backend REST DTO contracts (defined in active design docs).
- **Design Documents:** UI wireframes in design docs must remain **framework-agnostic**; during implementation, map them to standard Angular Material 3 components (`mat-card`, `mat-table`, `mat-dialog`, `mat-bottom-sheet`).

---

## 2. Invariants & Standards

- **Monetary Values:** Amounts received from the API are in integer minor units (`cents`). Format them to localized currency strings purely at the presentation boundary via pipes or formatting helpers.
- **Reactive State:** Manage state via RxJS (`BehaviorSubject`, `Observable`) or Angular Signals. Avoid mutable shared globals.
- **Authentication:** All outgoing `/api/v1/` HTTP requests must be intercepted by the auth interceptor to attach the Firebase ID token in the `Authorization: Bearer <token>` header.

---

## 3. Deterministic Verification Commands

Execute from the `/frontend` directory:

```bash
# Run headless unit tests
npm test -- --watch=false --browsers=ChromeHeadless

# Start local Angular development server (port 4200)
npm start

# Production build check
npm run build
```
