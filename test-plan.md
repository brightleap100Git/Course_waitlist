# Test Plan — Course Registration and Waitlist Management

## 1. Purpose and test strategy

This plan verifies the one-offering registration slice against [the requirements](./requirement.md), [the finalized ADRs](./architecture/architecture-decisions.md), and [the LLD](./architecture/lld.md). It covers returned outcomes and persisted in-memory state; checking responses alone is insufficient.

This is the planned test strategy and traceability baseline. For actual executed test names, results, coverage, and user-provided manual evidence, see [the verification report](./docs/VERIFICATION-REPORT.md) and [manual verification report](./docs/MANUAL-VERIFICATION-REPORT.md).

Use Java 21, Maven, and JUnit 5. When this plan was drafted, no starter application, database, external service, or web framework was available, so it specified tests for the permitted single-process in-memory fallback. That fallback has since been implemented; this plan remains the scenario and traceability baseline. Maven is installed directly in the development environment; do not add Maven Wrapper. Use JUnit assertions and small test fixtures only. Do not add a mocking, database, container, or property-testing framework.

### Test levels

| Level | Target | Purpose |
|---|---|---|
| Unit | Domain constraints, prerequisite validator, enrollment/drop service, waitlist ordering, promotion coordinator, in-memory repository | Verify isolated rules and outcomes with a fresh deterministic state per test. |
| Integration | Real service, repository, validator fixture, and minimal CLI wiring | Verify components compose correctly for enrollment, waitlisting, dropping, priority allocation, and errors. |
| Boundary and negative | Capacity and input validation, duplicate/repeated requests, eligibility failures | Verify edge cases produce clear outcomes and no unintended state change. |
| Failure handling | Controlled failures from eligibility checks or repository mutations/commit preparation | Verify failed multi-step workflows leave the pre-operation state unchanged and do not report success. |
| Concurrency | Multiple threads using the same service and in-memory state | Verify synchronization protects capacity, uniqueness, FIFO selection, and atomic workflow invariants. |

### Determinism and isolation

- Construct a fresh offering, repository, eligibility fixture, and service for each test. Do not share mutable state between tests.
- Use explicit ordered learner IDs and inspect waitlist positions/order directly.
- Use `CountDownLatch` to coordinate concurrent starts and `ExecutorService` to run bounded tasks; always release latches and shut down executors in `finally`.
- Concurrency assertions must concern outcomes and final invariants, not assume a particular thread wins a race. Where FIFO must be asserted, seed the queue in a known order before starting concurrent operations.
- Prefer a small fault-injecting in-memory repository/validator test double for failure cases. Do not add a mocking dependency.

## 2. Policy classification

The test cases below label their basis:

- **Assignment acceptance:** Direct verification of functional requirements and ACs in `requirement.md`.
- **Selected design policy:** Verification of a finalized ADR/LLD choice that resolves an assignment ambiguity or constrains implementation behavior. These tests supplement, and do not rewrite, the original assignment criteria.

The selected policies include deterministic FIFO; prerequisite checks on initial enrollment/waitlisting and again before promotion; skip and retain ineligible waitlisted learners at their original positions; consider existing eligible waitlisted learners before new enrollment requests; complete a successful drop with an available seat if no waiter is eligible; prevent duplicates and over-capacity atomically; and make in-memory drop-plus-promotion atomic on technical failure.

## 3. Test data and eligibility fixtures

Use an explicit fixture, separate from business logic:

| Fixture value | Meaning |
|---|---|
| `eligible-1`, `eligible-2`, `eligible-3`, `eligible-4` | IDs present in the in-memory set of eligible learners. |
| `ineligible-1`, `ineligible-2` | IDs absent from that set. |
| `null`, empty, whitespace-only ID | Invalid input examples at the request boundary. |
| Capacity `0`, `1`, and a small positive integer (for example `2`) | Capacity boundaries and concise multi-seat scenarios. |

The test fixture is not a real prerequisite rule or learner record. Unit tests may change fixture membership between initial waitlisting and promotion to simulate eligibility changing. For queue-specific scenarios that require an open seat alongside existing waiters, seed the repository state explicitly; this models the LLD's selected allocation policy, including a seat left open when queued candidates were previously ineligible.

## 4. Invariants checked after state-changing operations

After every successful state change, rejected request, and injected failure, assert all applicable invariants:

1. `capacity >= 0`.
2. `0 <= enrolledCount <= capacity`.
3. Each learner has at most one active registration in this offering.
4. No learner is both enrolled and waitlisted.
5. Only eligible learners are newly enrolled or initially waitlisted; promotion uses current eligibility.
6. Waitlist order remains deterministic FIFO. Skipped ineligible entries remain at their original positions.
7. An eligible existing waiter is allocated before a new request receives an available seat.
8. A successful promotion removes that learner from the waitlist and adds exactly one enrollment as a single consistent transition.
9. Invalid/duplicate/repeated requests do not mutate unrelated records.
10. A technical failure in an atomic operation preserves its entire pre-operation registration state and does not return a success-shaped result.

For deliberately invalid capacity construction, assert rejection before an offering/state store is accepted; invariants 2–10 apply to valid initialized offerings.

## 5. Detailed test cases

### 5.1 Unit, boundary, and negative cases

| ID | Type / basis | Preconditions | Action | Expected outcome | Traceability |
|---|---|---|---|---|---|
| TP-01 | Unit; assignment acceptance | Capacity `2`, no registrations, `eligible-1` in fixture, empty queue. | Enroll `eligible-1`. | Clear enrolled outcome; one enrollment; no waitlist entry. Invariants hold. | FR-01, FR-02; AC-01 |
| TP-02 | Unit; assignment acceptance | Capacity `1`, full; `eligible-1` is already first in the queue; `eligible-2` is eligible and not active. | Enroll `eligible-2`. | Clear waitlisted outcome; `eligible-2` is appended behind `eligible-1`; enrollment count remains `1`. | FR-04; AC-02 |
| TP-03 | Unit; assignment acceptance | Capacity `2`, `eligible-1` already enrolled. | Request enrollment for `eligible-1` again. | Clear duplicate/already-enrolled outcome; no new enrollment or waitlist entry. | FR-05; AC-03 |
| TP-04 | Unit; assignment acceptance | Capacity `1`, full; `eligible-2` is already waitlisted. | Request enrollment for `eligible-2` again. | Clear duplicate/already-waitlisted outcome; exactly one queue entry remains in the same position. | FR-05; AC-03 |
| TP-05 | Unit; assignment acceptance | Capacity `2`; `ineligible-1` absent from eligibility fixture; learner not active. | Request enrollment for `ineligible-1`. | Clear ineligible outcome; learner is neither enrolled nor waitlisted. | FR-03; AC-05, AC-06 |
| TP-06 | Unit; assignment acceptance | Capacity `1`, full; eligible learner not active. | Enroll learner. | Learner is added once at FIFO tail; capacity is not exceeded. | FR-04, FR-05; AC-02, AC-03 |
| TP-07 | Boundary; selected design policy | Capacity `0`; `eligible-1` is eligible and not active. | Enroll `eligible-1`. | No enrollment; learner joins waitlist once. State invariants hold. | FR-02, FR-04; AC-02, AC-09; capacity rule in LLD |
| TP-08 | Boundary/negative; selected design policy | No offering has yet been created. | Construct or configure capacity `-1`. | Reject with a clear configuration/input failure; no usable offering or registration state is created. | LLD capacity invariant; implementation-plan decision |
| TP-09 | Boundary/negative; selected design policy | No offering has yet been created. | Supply malformed, missing, or non-integer CLI capacity. | Reject input clearly; do not silently substitute a capacity or start registration. | FR-09; AC-07; implementation-plan decision |
| TP-10 | Negative; assignment acceptance | Valid offering; no learner has identifier `null`, empty, or whitespace-only. | Submit enrollment and drop requests with each invalid identifier. | Clear invalid-request outcomes; no enrollment, waitlist, or unrelated state changes. | FR-09; AC-07 |
| TP-11 | Unit; assignment acceptance | Capacity `1`, enrolled `eligible-1`. | Drop `eligible-1`. | Clear dropped outcome; enrollment removed and one seat released. | FR-06; AC-04 |
| TP-12 | Negative; assignment acceptance | Capacity `1`, no active enrollment for `eligible-2`; queue has a known entry. | Drop `eligible-2`. | Clear not-enrolled outcome; enrollment count and queue unchanged. | FR-09; AC-07, AC-08 |
| TP-13 | Negative; assignment acceptance | `eligible-1` was enrolled and successfully dropped; no active registration remains for them. | Drop `eligible-1` again. | Clear not-enrolled/repeated-drop outcome; no additional seat release or other state mutation. | FR-09; AC-08 |
| TP-36 | Negative; assignment acceptance | Capacity `1`, full; `ineligible-1` is not active and absent from the eligibility fixture. | Request enrollment for `ineligible-1`. | Clear ineligible outcome; learner is neither enrolled nor added to the waitlist; existing state is unchanged. | FR-03, FR-04; AC-05, AC-06 |

### 5.2 Promotion, FIFO, and design-policy cases

| ID | Type / basis | Preconditions | Action | Expected outcome | Traceability |
|---|---|---|---|---|---|
| TP-14 | Unit; assignment acceptance | Capacity `1`, occupied by `eligible-1`; queue order is `eligible-2`, `eligible-3`, both eligible. | Drop `eligible-1`. | `eligible-2`, the earliest eligible waiter, is enrolled; its entry is removed; `eligible-3` remains queued. | FR-06, FR-07; AC-04, AC-10 |
| TP-15 | Unit; assignment acceptance + selected policy | Capacity `1`, occupied; `ineligible-1` was eligible when queued but is now ineligible; queue order is `ineligible-1`, `eligible-2`, who remains eligible. | Drop enrolled learner. | Skip `ineligible-1`, retain it at its original position, promote `eligible-2`; never enroll `ineligible-1`. | FR-07, FR-08; AC-04 through AC-06; ADR-06 |
| TP-16 | Unit; selected policy | Capacity `1`, occupied; `ineligible-1` was eligible when queued but is now ineligible; no other candidates exist. | Drop enrolled learner. | Drop succeeds; ineligible entry remains in its original position; seat stays available; no promotion is reported. | FR-06, FR-08; AC-04, AC-05; ADR-06 |
| TP-17 | Unit; assignment acceptance + selected policy | Capacity `1`, occupied; `eligible-2` was eligible when waitlisted but is removed from the fixture before promotion. | Drop enrolled learner. | Eligibility is revalidated; learner is not promoted, remains in the original queue position, and the seat stays available if no other eligible candidate exists. | FR-07, FR-08; AC-05, AC-06; ADR-06, ADR-09 |
| TP-18 | Unit; assignment acceptance | Capacity `1`, occupied; queue has a known order with an ineligible head (previously eligible, now ineligible) and later eligible learners. | Drop enrolled learner. | Scan deterministically; promote the earliest currently eligible candidate, not merely the next queue entry; preserve every skipped entry and all relative ordering. | FR-07, FR-08; AC-04 through AC-06; ADR-05, ADR-06 |
| TP-19 | Unit; selected policy | Capacity `2`, one seat open after a prior allocation found no eligible candidate; `eligible-2` was queued while eligible, became ineligible when the seat was left open, and is now eligible; new `eligible-3` is not active. | Enroll `eligible-3`. | Allocate the seat to the existing eligible waiter first. If capacity is then full, `eligible-3` joins the queue tail; no over-capacity. | FR-02, FR-04; AC-01, AC-02; ADR-06; LLD-AC-01 |
| TP-20 | Unit; selected policy | Capacity `2`, one seat open after a prior allocation found no eligible candidate; queue order is `ineligible-1`, `eligible-2`; both were eligible when queued, `ineligible-1` is currently ineligible, and `eligible-2` is eligible; `eligible-3` is new. | Enroll `eligible-3`. | Skip but retain `ineligible-1`; promote `eligible-2` first; enqueue `eligible-3` if no seat remains. | FR-02, FR-07, FR-08; AC-01, AC-04 through AC-06; ADR-06; LLD-AC-02 |
| TP-21 | Unit; selected policy | Capacity `2`, one seat open after a prior allocation found no eligible candidates; all current waitlisted learners remain ineligible; `eligible-3` is eligible and new. | Enroll `eligible-3`. | Retain ineligible queue entries in place; since no eligible waiter takes the seat, enroll `eligible-3`. | FR-02, FR-08; AC-01, AC-05, AC-06; ADR-06; LLD-AC-03 |
| TP-22 | Unit; selected policy | Capacity `2`, one seat open after prior allocation found no eligible candidates; queued learners were previously ineligible and the fixture now marks multiple eligible; known FIFO order is seeded. | Allocate available seats. | Promote eligible waiters in FIFO order until seats are exhausted; preserve remaining waiters' order. | FR-07; AC-04, AC-10; ADR-05, ADR-06 |

### 5.3 Atomicity and failure-handling cases

| ID | Type / basis | Preconditions | Action | Expected outcome | Traceability |
|---|---|---|---|---|---|
| TP-23 | Failure handling; selected policy | Enrolled learner, eligible waiter, valid pre-operation state; test repository can inject a one-shot failure during promotion enrollment write. | Attempt drop and promotion; clear the injected fault and retry. | First attempt surfaces the technical failure and preserves the exact pre-operation state. Retry then commits the drop and FIFO promotion once; no duplicate, lost queue entry, or false success is visible. | FR-09; AC-07, AC-09; ADR-07; LLD transaction policy |
| TP-24 | Failure handling; selected policy | Enrolled learner and waitlist; fault can occur after staged drop or queue mutation but before atomic publication. | Inject failure during drop/promotion commit preparation. | Entire pre-operation state is retained; no partial drop or promotion; technical failure is surfaced. | FR-09; AC-07, AC-09; ADR-07 |
| TP-25 | Failure handling; selected policy | Open seat and existing eligible waiter; valid new eligible request; fault can occur during priority allocation. | Inject failure during allocation or staged enrollment/waitlist mutation. | No partial priority promotion or requester state is visible; prior state remains unchanged; failure is surfaced. | FR-05, FR-09; AC-03, AC-07, AC-09; ADR-03, ADR-07 |
| TP-26 | Failure handling; selected policy | Valid state; eligibility fixture/validator can throw a technical failure during promotion check. | Start drop-triggered promotion. | Technical failure is not treated as “ineligible”; atomic operation preserves pre-operation state and reports failure. | FR-08, FR-09; AC-05, AC-07, AC-09; ADR-07, ADR-09 |
| TP-27 | Negative; assignment acceptance | State snapshot recorded; submit a malformed enrollment or drop request. | Execute request. | Clear invalid result; exact registration and queue snapshot remains unchanged. | FR-09; AC-07, AC-09 |
| TP-28 | Unit; selected policy | Capacity `1`, enrolled learner and queue with no eligible waiter. | Perform a valid drop without injected technical failure. | Drop is committed and seat remains available; this expected policy outcome is not treated as a rollback-triggering failure. | FR-06; AC-04; ADR-06, ADR-07 |

### 5.4 Integration and concurrency cases

| ID | Type / basis | Preconditions | Action | Expected outcome | Traceability |
|---|---|---|---|---|---|
| TP-29 | Integration; assignment acceptance | Fresh CLI/service wiring, capacity `1`, fixture includes `eligible-1` and `eligible-2`. | Enroll first learner, enroll second, then drop first. | Outcomes show enrolled, waitlisted, then dropped with FIFO promotion; final state has one enrollment and no duplicate active registration. | FR-01 through FR-07; AC-01 through AC-04, AC-10 |
| TP-30 | Integration/negative; assignment acceptance | CLI configured with capacity `0` and documented eligibility set. | Submit valid eligible enrollment, invalid ID, and drop for absent learner. | Clear waitlisted, invalid, and not-enrolled results respectively; final state remains consistent. | FR-04, FR-09; AC-02, AC-07, AC-08, AC-09 |
| TP-31 | Concurrency; assignment acceptance + selected policy | Capacity `1`, no active record for `eligible-1`; all concurrent calls share the same service/repository. | Release multiple tasks through `CountDownLatch` to enroll the same learner simultaneously. | Exactly one active registration exists; no duplicate enrollment/waitlist entries; one call succeeds and others return duplicate or consistent outcomes. | FR-05; AC-03, AC-09, AC-11; ADR-03, ADR-04 |
| TP-32 | Concurrency; assignment acceptance | Capacity `C` (for example `2`), at least `C+1` distinct eligible learners and no queue; simultaneous enrollment tasks share state. | Start all tasks with a latch. | Enrolled count never exceeds `C`; each learner has at most one active record; eligible excess learners are waitlisted in the order actually serialized by the operation. Do not assert which racing caller wins. | FR-02, FR-04, FR-05; AC-01 through AC-03, AC-09, AC-11 |
| TP-33 | Concurrency; assignment acceptance + selected policy | Capacity `2`, one learner enrolled and one seat open; `eligible-2` was eligible when queued, became ineligible when the seat was left open, and is now eligible; `eligible-3` and `eligible-4` are new eligible requesters. | Seed state and fixture before the test; use `CountDownLatch` to race the two new enrollment requests. | The first serialized request considers and promotes `eligible-2` before its requester; the other new learner is handled against remaining capacity. Final state stays within capacity, and no new learner bypasses the existing eligible waiter. | FR-02, FR-07; AC-01, AC-04, AC-09, AC-11; ADR-06; LLD-AC-04 |
| TP-34 | Concurrency; assignment acceptance + selected policy | Capacity `2`, two distinct learners enrolled, and a known FIFO queue of eligible waiters. | Race drops of the two enrolled learners using `CountDownLatch` and shared service. | Each valid drop releases exactly one seat; promotions are serialized and FIFO among known queued candidates; no duplicate, missing, or partially transitioned registration. | FR-06, FR-07; AC-04, AC-09, AC-11; ADR-03, ADR-07 |
| TP-35 | Concurrency; assignment acceptance | `eligible-1` enrolled; no concurrent unrelated mutations. | Race multiple drops for `eligible-1`. | Exactly one drop succeeds; remaining calls return not-enrolled/repeated-drop outcome; no extra capacity change or promotion occurs. | FR-09; AC-08, AC-09, AC-11; ADR-03 |

## 6. Requirement-to-test traceability

| Source item | Verification |
|---|---|
| FR-01 | TP-01, TP-29 |
| FR-02 | TP-01, TP-07, TP-19 through TP-21, TP-32, TP-33 |
| FR-03 | TP-05, TP-36 |
| FR-04 | TP-02, TP-06, TP-07, TP-19, TP-20, TP-30, TP-32 |
| FR-05 | TP-03, TP-04, TP-25, TP-31, TP-32 |
| FR-06 | TP-11, TP-14 through TP-18, TP-23, TP-24, TP-28, TP-29, TP-34 |
| FR-07 | TP-14 through TP-20, TP-22, TP-29, TP-33, TP-34 |
| FR-08 | TP-15 through TP-18, TP-20, TP-21, TP-26 |
| FR-09 | TP-08 through TP-13, TP-23 through TP-27, TP-30, TP-35 |
| FR-10 | Test environment uses the specified no-starter fallback: Java 21, Maven, JUnit 5, CLI, single-process in-memory state; verify with `mvn test`. |
| AC-01 | TP-01, TP-19, TP-21, TP-32, TP-33 |
| AC-02 | TP-02, TP-06, TP-07, TP-19, TP-20, TP-30, TP-32 |
| AC-03 | TP-03, TP-04, TP-31, TP-32 |
| AC-04 | TP-11, TP-14 through TP-20, TP-23, TP-24, TP-28, TP-29, TP-33, TP-34 |
| AC-05 | TP-05, TP-15 through TP-18, TP-20, TP-21, TP-26, TP-36 |
| AC-06 | TP-05, TP-15 through TP-18, TP-20, TP-21, TP-26, TP-36 |
| AC-07 | TP-08 through TP-10, TP-23 through TP-27, TP-30 |
| AC-08 | TP-12, TP-13, TP-30, TP-35 |
| AC-09 | State invariants after all mutation/rejection cases; explicitly TP-01 through TP-36 as applicable, especially TP-23 through TP-27 and TP-31 through TP-36. |
| AC-10 | TP-01 through TP-06, TP-14, TP-15, TP-18, TP-29 |
| AC-11 | TP-31 through TP-35; concurrency test is included because this selected implementation is synchronized. |
| AC-12 | Manual deliverable check: verify AI-use record contains actual prompts/use summary, accepted/modified/rejected dispositions, affected artifacts, and independent verification evidence. This is documentation review, not an application behavior test. |
| ADR-03 / ADR-04 | TP-23, TP-25, TP-31 through TP-35; atomic capacity and duplicate enforcement. |
| ADR-05 / ADR-06 | TP-14 through TP-22, TP-33, TP-34; deterministic FIFO, skip-and-retain, and existing-waiter priority. |
| ADR-07 | TP-23 through TP-26, TP-28, TP-34; atomic state transitions, rollback on technical failures, open seat on successful no-candidate drop. |
| ADR-08 | TP-03, TP-04, TP-08 through TP-13, TP-23 through TP-27, TP-30, TP-35; explicit expected results and surfaced technical failures. |
| ADR-09 | TP-05, TP-15 through TP-17, TP-20, TP-21, TP-26; demo fixture and eligibility revalidation. |
| LLD-AC-01 | TP-19 |
| LLD-AC-02 | TP-20 |
| LLD-AC-03 | TP-21 |
| LLD-AC-04 | TP-31 through TP-34 |

## 7. Execution commands

Install Maven directly in the development environment and use Java 21. Do not add or invoke Maven Wrapper.

```text
mvn test
```

Optional focused runs after test classes are implemented:

```text
mvn -Dtest=EnrollmentServiceTest test
mvn -Dtest=PromotionCoordinatorTest test
mvn -Dtest=RegistrationIntegrationTest test
mvn -Dtest=RegistrationConcurrencyTest test
```

The class names are proposed test organization, not existing files. The full `mvn test` run is the required final suite; report the actual command and result rather than assuming success.

## 8. Entry and exit criteria

### Entry criteria

- Java 21 and directly installed Maven are available.
- The project has its Maven/JUnit 5 configuration and compiles.
- The in-memory offering/repository, service boundary, and eligibility fixture can be constructed deterministically.
- The finalized policy is implemented or the test is explicitly staged for the implementation step that introduces it.
- Test cases can inspect enrollment count, active registration, waitlist order, and outcomes without external infrastructure.

### Exit criteria

- `mvn test` passes all unit, integration, boundary, negative, failure-handling, and concurrency tests.
- All assignment ACs (AC-01 through AC-12) have traceable verification, including manual evidence review for AC-12.
- All selected-policy cases (LLD-AC-01 through LLD-AC-04 and relevant ADRs) pass.
- Invariants hold after each successful operation, expected rejection, and injected technical failure.
- Concurrency tests are bounded, deterministic in their assertions, and leave no executor threads running.
- Test results are repeatable from a clean test state; no database, network service, real user data, or test-only production behavior is required.

## 9. Known limitations, missing coverage, and unresolved test assumptions

- The assignment does not define a real prerequisite rule or source of learner data. Tests use the finalized in-memory eligible-ID demo fixture, separated from business logic; this validates policy flow, not a real academic prerequisite integration.
- Numeric capacity is unspecified by the assignment. The finalized implementation accepts integer capacity `>= 0`; capacity zero and negative/invalid inputs are covered as boundary/design-policy tests.
- The exact CLI syntax, identifier validation format beyond the minimal invalid examples, and exact result/message text are implementation details. Tests should assert stable outcome categories and state, not incidental wording.
- In-memory synchronization tests establish single-process behavior only; they do not prove cross-process coordination, persistence, crash recovery, or database transaction behavior. These are outside this design.
- FIFO under concurrency is defined by the order in which the synchronized operation admits requests, not by thread creation time or latch release order. Tests must not assert an unknowable scheduler ordering.
- Failure atomicity tests require controllable failure injection at validator/repository mutation or commit preparation boundaries. This should be implemented as test support, not as a production failure-management feature.
- Assignment AC-04/FR-07 specify promotion of the earliest eligible waiter; the selected skip-and-retain policy and eligibility revalidation are policy choices documented in ADR-06/ADR-09. TP-15 through TP-18 verify those choices without treating them as original assignment text.
- FR-02/AC-01 remain unchanged. The selected ADR-06 design assumption means existing eligible waitlisted learners receive available seats first; “capacity remains” for a new request is evaluated after that allocation. TP-19 through TP-21 are supplemental design-policy tests, not amendments to AC-01.
- No contradiction blocks testing. The assignment's open queue-head and eligibility-timing questions are resolved by selected ADRs. No external framework, infrastructure, or database is assumed.
