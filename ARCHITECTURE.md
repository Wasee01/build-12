# Architecture

## 1. Scope

Java 21, Spring Boot 4, Spring MVC, Spring Security, Spring AOP, Spring Retry,
Spring Data JPA, MySQL 8.4, and Flyway. The project deliberately uses MySQL,
not an embedded H2 database. This small sample models enroll, drop, and status
change. Its demo users are not a production identity provider.

## 2. Dependencies and responsibilities

- **EnrollmentController**: HTTP adapter; invokes enrollment and notification services.
- **EnrollmentService**: transaction-scoped domain writes and authorization rules.
  It has **no audit writer, repository, entity, or logging statements**.
- **AuditAspect**: cross-cutting `@Around` advice and security-context principal lookup.
- **AuditWriter**: independent `REQUIRES_NEW` transaction for each audit row.
- **NotificationService**: outbound retry and terminal recovery.
- **Flyway**: `V1` enrollment, `V2` audit_log, `V3` notification_failure.

## 3. Example request lifecycle

`POST /api/enrollments` -> HTTP Basic security -> controller -> proxied
`EnrollmentService.enroll(sectionId)` -> audit advice -> method authorization ->
transaction advice -> method body -> transaction commit/rollback -> audit write
in a distinct transaction -> controller -> retryable notification -> response.

A bad section ID throws `IllegalArgumentException`. The transaction rolls back,
then the aspect records FAILURE separately and **rethrows the original exception**.
The error is returned as HTTP 400 by the exception handler.

## 4. AOP, transactions, and architectural justification

### Pointcut expressions in plain English

- `@Around("@annotation(auditedOperation)")` means: intercept any method that
  carries `@AuditedOperation`; bind that annotation to `auditedOperation` so the
  advice can read `action`, `targetType`, and `targetIdArgument`.
- `execution(* com.example.auditing.enrollment..*(..))` would mean: intercept
  any method inside the enrollment package or its child packages, regardless
  of its name or return type. **This is an illustrative alternative, not active.**
- `args(java.lang.Long,..)` would mean: intercept eligible method calls whose
  first runtime argument is a `Long`. **Illustrative only, not active.**
- Using an annotation rather than a package-wide pointcut limits audit scope
  exactly to designated business mutations.

### What `@Transactional` has done since Week 9

`@Transactional` is transaction advice, normally applied through a Spring proxy.
A call **through the proxy** begins or joins a transaction, executes the target
method, commits on normal completion, and rolls back on unchecked exceptions
and errors (by default). Database changes are atomic inside that transaction.
Neither a JDBC annotation nor writing `@Transactional` automatically creates a
transaction if the call bypasses the proxy.

**Self-invocation**: `this.changeStatus(...)` from another method of the same
service is a direct call on the target object, **not through Spring's proxy**.
Therefore `@Transactional`, `@AuditedOperation`, `@Retryable`, and proxy-based
method security on that *inner method* are not independently applied. If the
outer method already has a transaction, the inner method executes within the
outer transaction, but does not get its own transaction boundary/propagation.
Fix: move the called operation to a separately injected Spring bean, or use
`TransactionTemplate` for explicit transaction management. AspectJ weaving is
another option with different setup. We never rely on self-invocation here.

### Ordering and rollback-surviving audit records

`AuditAspect` uses `@Order(Ordered.HIGHEST_PRECEDENCE)`, making it outermost
relative to normal transactional advice. It calls `proceed()`; the transaction
advice opens/commits/rolls back the enrollment transaction before returning
or rethrowing to the audit aspect. Afterward, `AuditWriter` writes via a
`TransactionTemplate` configured with `PROPAGATION_REQUIRES_NEW`.

**Decision:** failed attempts are deliberately persisted even if the business
operation rolls back. This helps investigation and detects repeated abuse.
The audit row does not participate in the business rollback. In general a
`REQUIRES_NEW` write suspends an existing transaction; it needs a second DB
connection if the outer transaction is still open, so size the pool accordingly.

**Tradeoffs:** Audit writes are synchronous and fail-closed on the successful
path; if the audit store is down, a successfully committed business mutation
could already exist while the request reports failure. For strict atomic
business/audit success, use a transactional outbox and durable publisher.
The code also cannot guarantee an audit for a process crash or database outage.
For errors before entering the proxied service (such as HTTP parsing or a
firewall rejection), this method-level aspect does not run.

### Why auditing belongs in an aspect

EnrollmentService should implement enrollment rules, not security-event
persistence. Auditing is cross-cutting and must behave consistently on both
success and exception paths. One pointcut avoids duplicated try/catch blocks,
ensures every annotated mutation follows the same schema, and allows auditing
to evolve without touching business services. The only audit-specific service
lines are the three `@AuditedOperation` declarations.

### Retry semantics

The *public* method `NotificationService.sendEnrollmentNotice` is called by
another Spring bean (controller) through the retry proxy. `@Retryable` attempts
up to three calls (the initial attempt plus two retries), waiting 100 ms
between attempts. A matching `@Recover(RuntimeException, Long)` is invoked only
when all attempts fail; it records the terminal failure in
`notification_failure` and returns normally. Recovery does not mask failure
silently: operators can inspect the failure table. Neither the notification
success nor provider availability is used to decide enrollment commit.

## 5. Diff proof and verification

This is a greenfield repository, so there is **no preexisting service revision**
from which to generate a truthful before/after `git diff`. The architecture
rule is verified by `python scripts/check-audit-boundary.py` and is enforced
in CI. It proves the service has exactly three audit annotations and none of
the audit writer/entity/repository implementation. After checking in a
baseline commit, use `git diff <baseline>..HEAD -- src/main/java/com/example/auditing/enrollment/EnrollmentService.java`
to demonstrate annotation-only changes in a later revision.

Run `mvn clean verify` with MySQL running; `EnrollmentAuditIntegrationTest`
checks real persistence, success/failure counts, principal, drop/status,
and retry/recover. `AuditAspectTest` isolates exception handling.
