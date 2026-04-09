# Enterprise Code Review – CCN Integration Service

## Scope and understanding

This review was created after tracing the primary request and queue-processing flow through:

- `CcnIntegrationController` (entry point for synchronous API and queue-status endpoint)
- `OrchestrationService` (application routing and destination resolution)
- `CountryMappingService` (country → destination lookup)
- `CcnIntegrationLayer` (queue configuration resolution + send/browse logic)
- `CcnQueue` (NJCsi send, browse, metrics, and receiver delegation)
- `CcnIntegrationServiceConfiguration` + `CsiConfigProperties` (application bootstrap and dynamic bean maps)

## Current architecture (what exists)

- A **single module** Spring Boot service.
- Heavy use of **runtime map-based wiring** (`Map<String, ...>`) for per-application behavior.
- Message transport and integration logic are mixed with orchestration and infrastructure concerns.
- Inbound and outbound flows are both handled inside similar classes without strict port boundaries.
- Configuration is feature-rich, but modeled as deeply nested property classes and assembled manually.

## Critical maintainability findings

### 1) Build and dependency management fragility

- The build currently cannot resolve the Spring Boot parent POM in this environment (403 from Maven Central).
- Dependency management is incomplete/outdated in places (manual micrometer version pin that may diverge from Boot BOM).

**Enterprise risk:** unreproducible builds and inconsistent dependency trees across environments.

### 2) Compilation and type-contract issues in core flow

- Cross-package import inconsistencies and duplicate exception import patterns are present.
- Several classes reference types without explicit imports and rely on broad context assumptions.
- `CcnQueue` has incomplete methods (`buildResponse`, `buildEnqueuedMessage`) and an incorrect return type for response building.
- Exception constructors are defined but do not call `super`, dropping error context.

**Enterprise risk:** fragile CI/CD, runtime defects, and poor diagnosability.

### 3) Weak domain boundaries (layering violations)

- Transport adapters, orchestration, and integration concerns are tightly coupled.
- `OrchestrationService` and `CcnIntegrationLayer` expose raw stringly-typed APIs (`applicationType`, `messageType`, `countryCode`).
- No explicit domain model for message command, destination, envelope metadata, or queue operation results.

**Enterprise risk:** difficult onboarding, harder change impact analysis, high regression surface.

### 4) Configuration assembly complexity and hidden side-effects

- `CcnIntegrationServiceConfiguration` creates many object graphs manually and repeatedly calls bean methods.
- Validation is done ad hoc during map construction rather than centralized and fail-fast.
- Scheduler setup pulls dependencies dynamically and can hide lifecycle/order issues.

**Enterprise risk:** startup surprises, poor observability of config failures, and difficult testing.

### 5) Error-handling and API contract maturity gaps

- Controller methods throw generic exceptions and return fixed success responses regardless of nuanced failure modes.
- No unified RFC7807 Problem Details strategy or consistent error code catalog.
- No global exception mapping strategy for business/integration/validation faults.

**Enterprise risk:** inconsistent client experience and non-actionable incident telemetry.

### 6) Security and compliance concerns

- Config includes credentials-like values in plaintext examples.
- Input validation annotations are inconsistent and may be using non-standard packages.
- Missing explicit redaction policy for log fields (message IDs/correlation IDs may be sensitive in regulated contexts).

**Enterprise risk:** compliance findings and audit issues.

### 7) Testing and quality gates are not evident

- No visible unit/integration contract tests around orchestration, mapping, and queue adapters.
- No architecture tests (layer dependency rules) or mutation/coverage gates.

**Enterprise risk:** unsafe refactors and slow delivery confidence.

## Recommended enterprise target design

### A) Move to Hexagonal/Clean architecture boundaries

Create explicit packages/modules:

- `domain`
  - entities/value objects (`ApplicationType`, `MessageType`, `CountryCode`, `Destination`)
  - domain services and policies
- `application`
  - use-cases (`SendMessageUseCase`, `CheckQueueStatusUseCase`)
  - commands/results DTOs (internal)
- `infrastructure`
  - NJCsi adapter, REST callback adapter, config persistence adapter
- `interfaces`
  - REST controllers, request/response mappers

**Rule:** controllers call use-cases; use-cases depend on ports; adapters implement ports.

### B) Introduce ports and typed contracts

Define interfaces:

- `MessageGatewayPort` (send, browse/checkDepth)
- `DestinationResolverPort`
- `InboundMessageDispatchPort`
- `ClockPort` and `IdGeneratorPort` (testability)

Replace string arguments with immutable command objects (e.g., `SendMessageCommand`).

### C) Centralize configuration and validation

- Split configuration by concern (`GatewayProperties`, `QueueProperties`, `RoutingProperties`).
- Use `@Validated` on configuration properties with Bean Validation constraints.
- Add startup validator that checks cross-reference integrity (`replyTo` exists, message type uniqueness).
- Build immutable registry objects once during startup.

### D) Standardize error model and observability

- Add `@RestControllerAdvice` with RFC7807 `ProblemDetail` responses.
- Create explicit exception taxonomy:
  - `ValidationException`, `RoutingException`, `GatewayUnavailableException`, `TransportException`.
- Enrich logs with structured key-value fields and correlation context (MDC).
- Expand metrics: queue depth, browse lag, send latency percentiles, failure counters by cause.

### E) Security hardening baseline

- Externalize secrets via Vault/KMS or environment-backed secret managers.
- Add secure logging policy (masking) and static checks for sensitive data leaks.
- Enable actuator security and health group segregation.

### F) Testing strategy (enterprise minimum)

- Unit tests for use-cases and mapping logic.
- Adapter tests for NJCsi wrapper with fake/stub boundaries.
- Spring slice tests for controller + exception handler.
- Contract tests for outbound/inbound message formats.
- Architecture tests (ArchUnit) to enforce dependency boundaries.

## Phased migration plan (low-risk)

### Phase 1 – Stabilize and enforce quality

1. Fix compile-time issues and incomplete methods.
2. Add Checkstyle/SpotBugs + formatting + CI quality gates.
3. Introduce global exception handler and consistent API error schema.
4. Add baseline unit tests for orchestration + country mapping.

### Phase 2 – Introduce abstraction seams

1. Add typed command/result objects for send and queue status.
2. Extract ports from current `CcnIntegrationLayer` and `CcnQueue` dependencies.
3. Isolate map-based registry behind a dedicated `ApplicationRegistry`.

### Phase 3 – Repackage by architecture

1. Move code into `interfaces/application/domain/infrastructure` packages.
2. Keep existing behavior with adapter wrappers.
3. Add architecture tests to prevent relapse.

### Phase 4 – Operability and resilience

1. Add retry/circuit-breaker policies for remote calls.
2. Add dead-letter/poison-message handling policy.
3. Add SLO-backed dashboards and alerting playbooks.

## Suggested project structure (example)

```text
src/main/java/ie/revenue/ccn/integration
  interfaces/rest
    CcnIntegrationController
    GlobalExceptionHandler
    dto/
  application
    usecase/
    command/
    result/
    port/
  domain
    model/
    service/
    exception/
  infrastructure
    njcsi/
    rest/
    config/
    metrics/
```

## Immediate high-value changes to prioritize

1. Resolve compile and import inconsistencies before any feature work.
2. Fix exception constructors to preserve messages and causes.
3. Create global exception translation and remove generic `throws Exception` in controller.
4. Replace stringly-typed method signatures with validated command objects.
5. Extract queue transport into a dedicated adapter interface and test harness.
6. Add CI gates: unit tests, style checks, and architecture enforcement.

---

If useful, the next step can be a concrete implementation PR that performs **Phase 1 only** (no behavioral redesign) so the service reaches a clean, testable baseline before structural migration.
