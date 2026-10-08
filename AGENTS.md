# Agent Guidelines: Expense Tracker Monorepo

> Full-stack multi-currency household expense tracking monorepo deployed on Google Cloud Run and Firebase Hosting.

## 📖 Progressive Disclosure Links

- **Design Philosophy & Complexity (APoSD):** [docs/DESIGN_PHILOSOPHY.md](docs/DESIGN_PHILOSOPHY.md)
- **Backend Architecture & Guidelines (Spring Boot / Firestore):** [backend/AGENTS.md](backend/AGENTS.md)
- **Frontend Architecture & Guidelines (Angular / Material 3):** [frontend/AGENTS.md](frontend/AGENTS.md)
- **Active Feature Designs:** [docs/designs/](docs/designs/)

---

## 1. Feature Lifecycle Protocol

1. **Design First:** Before writing code for any non-trivial feature, check `/docs/designs/` for an active design doc or draft a new one in `/docs/designs/<feature_name>.md`.
2. **API Contract First:** Always define the REST endpoint URL, HTTP method, request payload, and response JSON structure in the design doc before writing Java or TypeScript code.
3. **Execution Order:** Implement and test backend REST endpoints first -> Implement Angular services and UI components to consume the API second.
4. **Lifecycle Completion:** Once full-stack functionality is verified and tested, move the design doc from `/docs/designs/` to `/docs/archive/` and update `README.md`.

---

## 2. Core Project Invariants (Do Not Violate)

- **Currency & Money:** ALWAYS store and transmit monetary values in integer minor units (`cents`, e.g., `$10.50` -> `1050`). NEVER use floating-point `double` or `float` for monetary amounts.
- **Deep Modules:** Follow [docs/DESIGN_PHILOSOPHY.md](docs/DESIGN_PHILOSOPHY.md)—build deep modules with simple public APIs, hide storage details, and define errors out of existence.
- **API Standard:** All backend REST controller paths must start with `/api/v1/`.
- **Authentication:** All `/api/v1/*` endpoints require a valid Firebase ID token in `Authorization: Bearer <token>`.
- **Framework-Agnostic Designs:** Keep UI wireframes and domain specifications in design docs framework-agnostic.
- **Documentation & Terminology:** Adhere to the [Google Developer Documentation Style Guide Word List](https://developers.google.com/style/word-list) across all design docs, UI microcopy, and error messages (e.g. use "sign in" not "login", "select" not "click", "set up" vs "setup").

---

## 3. Deterministic Verification Commands

```bash
# Run backend tests
cd backend && ./gradlew test

# Run frontend tests (headless)
cd frontend && npm test -- --watch=false --browsers=ChromeHeadless

# Full monorepo build check
cd backend && ./gradlew build -x test && cd ../frontend && npm run build
```
