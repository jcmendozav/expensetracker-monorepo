# Agent Guidelines: Expense Tracker Monorepo

## Project Overview
This repository is a full-stack monorepo containing a Spring Boot backend (`/backend`) and an Angular frontend (`/frontend`) deployed on Google Cloud Platform (GCP).

---

## 1. Requirement & Design Workflow
- **Design First:** Before writing code for any non-trivial feature, check `/docs/designs/` for an active design doc or draft a new one.
- **API Contract First:** Always define the REST endpoint URL, HTTP method, request payload, and response JSON structure in the design doc before writing Java or TypeScript code.
- **Lifecycle:** 
  1. Create design doc in `/docs/designs/ISSUE_NAME.md`.
  2. Implement backend (`/backend`) and frontend (`/frontend`).
  3. Once verified, move the design doc to `/docs/archive/` and update `README.md`.

---

## 2. Backend Guidelines (`/backend`)
- **Stack:** Java 17+, Spring Boot, Google Cloud Firestore.
- **API Standard:** All REST controller paths must start with `/api/v1/`.
- **Database:** Mapped directly to GCP Cloud Firestore collections using Spring services/repositories.
- **DTOs:** Create explicit Data Transfer Objects for incoming requests and outgoing responses.
- **Testing & Verification:** Run tests from the `/backend` directory:
  ``bash
  cd backend && ./mvnw test
  ``

---

## 3. Frontend Guidelines (`/frontend`)
- **Stack:** Angular (latest), Angular Material, RxJS.
- **Architecture:** 
  - Components must remain presentational (dumb UI).
  - Business logic, state, and HTTP calls belong in Angular Services.
  - TypeScript interfaces must strictly match the Spring Boot API contract DTOs.
- *(Testing & Verification:** Run tests from the `/frontend` directory:
  ``bash
  cd frontend && npm test -- --watch=false
  ``c

---

## 4. Full-Stack Verification Protocol
When completing a full-stack task:
1. Implement and test the Spring Boot REST endpoints first.
2. Implement the Angular services and ui components to consume the API.
3. Verify bothlayers build and pass unit/integration checks before committing.
