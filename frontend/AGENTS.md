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

## 3. Unidirectional Data Flow & Change Detection Safety

To permanently eliminate `ExpressionChangedAfterItHasBeenCheckedError`, infinite Change Detection loops, data-view inconsistencies, and hard-to-debug side effects:

- **Data Down, Events Up:**
  - Data flows downward strictly via `input()` signals and read-only service observables/signals.
  - Events and user intents flow upward strictly via `output()` events or explicit service method invocations (`service.update(...)`).
  - Child components must **NEVER directly mutate parent or ancestor state**.
- **Pure Render Expressions & `computed()`:**
  - Template bindings and `computed()` signals must remain **100% pure and side-effect free**.
  - NEVER trigger HTTP requests, route navigation, or signal mutations inside a template expression, getter, or `computed()`.
- **Zero View-Phase State Mutations:**
  - NEVER modify component or parent state inside `ngAfterViewInit`, `ngAfterViewChecked`, `ngAfterContentInit`, or `ngAfterContentChecked`.
  - Initialize state and subscriptions in `ngOnInit` or constructor signal initializers.
- **Signal Mutation Integrity:**
  - Do NOT use `effect()` to synchronize state between signals (use `computed()` or `linkedSignal()`).
  - Keep `effect()` strictly for external I/O (logging, analytics, third-party non-Angular DOM plugins).
  - Use immutable updates: `signal.update(list => [...list, newItem])`.
- **Zero Tolerance for Band-Aid CD Hacks:**
  - NEVER use `setTimeout(() => ...)` or `ChangeDetectorRef.detectChanges()` to bypass `ExpressionChangedAfterItHasBeenCheckedError`. If the error occurs, correct the directional flow of data at the architectural source.

---

## 4. TypeScript & Async Quality Standards (`typescript-eslint`)

Adhere strictly to high-reliability TypeScript and ESLint standards:

- **Zero `any` (`no-explicit-any`):** The `any` type is **strictly banned**.
  - Use `unknown` for uncertain inputs/boundaries and narrow with type guards (`instanceof`, `typeof`).
  - Catch clauses must always use `catch (err: unknown)`:

    ```typescript
    try {
      await this.budgetService.createBudget(payload);
    } catch (err: unknown) {
      if (err instanceof HttpErrorResponse) {
        this.errorMessage.set(err.error?.message ?? 'Failed to create budget');
      }
    }
    ```

- **No Floating Promises (`no-floating-promises`):**
  - All Promises must be `await`ed, attached to a `.catch()`, or explicitly marked `void` (e.g. `void this.router.navigate(['/']);`).
  - Never allow asynchronous calls to float unhandled, risking silent background failures.
- **Nullish Coalescing over Logical OR (`prefer-nullish-coalescing`):**
  - Always use `??` instead of `||` for fallback values.
  - In monetary contexts, `0` cents is a valid amount; `||` treats `0` as falsy and corrupts amounts to the fallback.
- **Immutability of Injected Dependencies (`prefer-readonly`):**
  - Mark all private service injections and read-only signal fields as `readonly`:

    ```typescript
    private readonly budgetService = inject(BudgetService);
    readonly budgets = this.budgetService.budgets;
    ```

- **Consistent Type Imports (`consistent-type-imports`):**
  - Use `import type { Budget } from '...'` for type-only imports to optimize bundler tree-shaking and avoid circular runtime dependencies.

---

## 5. Financial Invariants & Authentication

- **Monetary Minor Units:** Amounts from the REST API are in integer minor units (`cents`). Format into localized currency strings only at the presentation boundary (via pipes/formatting utilities).
- **Authentication:** All outgoing `/api/v1/*` HTTP calls must include the Firebase ID token in `Authorization: Bearer <token>` via the HTTP interceptor.

---

## 6. Deterministic Verification Commands

Execute from the `/frontend` directory:

```bash
# Run headless unit tests
npm test -- --watch=false --browsers=ChromeHeadless

# Start local Angular development server (port 4200)
npm start

# Production build check
npm run build
```
