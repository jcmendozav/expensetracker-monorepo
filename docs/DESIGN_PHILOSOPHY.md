# Software Design Philosophy & Complexity Management

> Based on the principles from *A Philosophy of Software Design* by John Ousterhout.  
> These guidelines are **framework-agnostic** and apply universally across all layers (Backend, Frontend, Data, and Architecture).

---

## 1. The Nature of Complexity

Complexity is incremental: it accumulates in small drops through tactical shortcuts, leaky abstractions, and excessive fragmentation. Our engineering goal is **Strategic Programming**—investing continuous effort to maintain a clean, durable design rather than applying quick tactical patches that accrue technical debt.

---

## 2. Core Design Principles

### Principle 1: Deep Modules over Shallow Modules

- **Deep Module:** A module (class, service, or component) that has a **simple, narrow interface** while encapsulating **substantial, complex functionality**.
- **Shallow Module:** A module whose interface is complex relative to the small amount of functionality it provides (e.g., pass-through wrappers or 1-line delegators that do nothing but pass arguments along).
- **Rule:** Maximize the depth of modules. Avoid creating classes or abstractions that do not genuinely simplify the mental model for the caller.

```
       ┌──────────────────────────────┐
       │   Simple Public Interface    │  <-- Deep Module: Low cognitive load for caller
       ├──────────────────────────────┤
       │                              │
       │  Substantial Implementation  │  <-- Hides validation, transactions, caching,
       │     & Business Invariants    │      and internal state transformations
       │                              │
       └──────────────────────────────┘
```

---

### Principle 2: Information Hiding & Encapsulation

- **Rule:** A module should encapsulate its internal mechanisms so that callers do not know, assume, or depend on how that information is stored, fetched, or transformed.
- **Avoid Information Leakage:** If changing a database schema, storage layout, or internal data representation forces changes across multiple unrelated files, the abstraction has leaked.
- **Boundary Separation:** Domain models and external API/storage models must remain decoupled. Internal representation changes should never cascade across domain boundaries.

---

### Principle 3: Define Errors Out of Existence

- **Rule:** The best way to reduce error-handling complexity is to design APIs and operations so that edge cases and boundary conditions are handled naturally without throwing exceptions or requiring defensive checks from the caller.
- **Techniques:**
  - **Idempotency:** Designing operations (such as voiding, canceling, or updating state) so that repeating them produces the desired end state safely without failing.
  - **Safe Defaults & Null Object Patterns:** Returning meaningful empty collections or fallback states rather than throwing null or generic lookup exceptions where a zero-result is valid domain behavior.
  - **Graceful Boundary Handling:** Normalizing inputs and handling ranges cleanly rather than rejecting slightly imperfect yet unambiguous requests.

---

### Principle 4: Separation of General-Purpose and Special-Purpose Code

- **Rule:** Build core domain logic to be **somewhat general-purpose**, while keeping specialized workflows in orchestration or presentation layers.
- If a service method is too tightly coupled to a single specific UI screen's temporary layout needs, split the reusable domain operations from the UI-specific aggregation.

---

### Principle 5: High-Signal Documentation (The "Why", Not the "What")

- **Rule:** Comments and documentation should capture non-obvious design decisions, business invariants, cross-module dependencies, and trade-offs.
- **Anti-Pattern:** Echoing what code already plainly says (e.g., `// sets user id`).
- **Best Practice:** Document *why* a particular invariant exists (e.g., `// Transaction amounts are stored in integer minor units (cents) to eliminate IEEE 754 floating-point rounding errors in multi-currency ledger reconciliations.`).

---

## 3. Checklist for Code Review & Self-Correction

Before finalizing any module or API:

- [ ] Is this module **deep**? Does the public API hide significant complexity?
- [ ] Is internal knowledge (storage shape, third-party libraries) **hidden** from callers?
- [ ] Have we **defined errors out of existence** where sensible, rather than multiplying try/catch blocks?
- [ ] Is this implementation **strategic** (clean and durable) rather than merely tactical (a quick hack)?
