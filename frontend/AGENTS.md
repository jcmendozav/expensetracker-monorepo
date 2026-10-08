# Frontend Guidelines: Expense Tracker (`/frontend`)

> Angular SPA (v20+) & Angular Material 3 client application.

## 1. Design & Architecture Principles

- **Design Philosophy:** Adhere strictly to [docs/DESIGN_PHILOSOPHY.md](../docs/DESIGN_PHILOSOPHY.md).
  - Create **deep stateful Angular services** that encapsulate HTTP communication, reactive state management, and data transformations.
  - Keep UI components **presentational (dumb)**—focused strictly on template rendering, user event emission, and view concerns.
- **API Contracts:** TypeScript interfaces must strictly match the Spring Boot backend REST DTO contracts (defined in active design docs).
- **Design Documents:** UI wireframes in design docs must remain **framework-agnostic**; during implementation, map them to standard Angular Material 3 components (`mat-card`, `mat-table`, `mat-dialog`, `mat-bottom-sheet`).

---

## 2. Modern Angular (v20+) Coding Standards (Official AI Guidelines)

### Component Authoring & Reactivity

- **Signals First:** Use Angular Signals (`signal()`, `computed()`, `linkedSignal()`) for all component and service state.
  - Use `input()` and `output()` functions instead of `@Input()` and `@Output()` decorators.
  - Use `model()` for two-way bindings with `[(prop)]` syntax.
  - Use `computed()` for derived state; never write manual side-effect getters.
  - Do NOT use `mutate` on signals; use `set()` or `update()`.
- **Standalone:** Standalone components are the default in Angular 20+. Do NOT specify `standalone: true` in decorators.
- **Dependency Injection:** Always use the `inject(Service)` function instead of constructor parameter injection.
- **Host Bindings:** Do NOT use `@HostBinding()` or `@HostListener()`. Specify host bindings inside the `host` property of the `@Component` metadata:

  ```typescript
  @Component({
    selector: 'app-budget-card',
    host: {
      '[class.over-budget]': 'isOverBudget()',
      '(click)': 'onCardClick()'
    }
  })
  ```

### Templates & Clean Imports

- **Native Control Flow:** ALWAYS use native control flow (`@if`, `@for`, `@switch`). NEVER use legacy `*ngIf`, `*ngFor`, or `*ngSwitch`.
  - Always provide `track` in `@for` (e.g. `@for (item of items(); track item.id)`).
- **Bindings:** Use native `[class.name]="..."` and `[style.name]="..."` bindings. Do NOT use `[ngClass]` or `[ngStyle]`.
- **Selective Imports:** Do NOT import `CommonModule`. Import only the specific directives or pipes needed in the component (e.g. `AsyncPipe`, `DatePipe`, `CurrencyPipe`, `NgOptimizedImage`).
- **Static Images:** Use `NgOptimizedImage` for static images.

### Forms & Accessibility

- **Forms:** Use Reactive Forms (`FormBuilder`, `FormGroup`, `FormControl`) with type-safe controls.
- **Accessibility (a11y):** Ensure all components meet WCAG AA standards, support keyboard navigation, include proper ARIA attributes, and maintain proper color contrast.

---

## 3. Financial Invariants & Authentication

- **Monetary Minor Units:** Amounts from the REST API are in integer minor units (`cents`). Format into localized currency strings only at the presentation boundary (via pipes/formatting utilities).
- **Authentication:** All outgoing `/api/v1/*` HTTP calls must include the Firebase ID token in `Authorization: Bearer <token>` via the HTTP interceptor.

---

## 4. Deterministic Verification Commands

Execute from the `/frontend` directory:

```bash
# Run headless unit tests
npm test -- --watch=false --browsers=ChromeHeadless

# Start local Angular development server (port 4200)
npm start

# Production build check
npm run build
```
