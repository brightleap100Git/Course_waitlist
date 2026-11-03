# Implementation Plan — Course Registration and Waitlist Management

## 1. Existing project assessment

This section records the initial planning-time inspection, before the Java implementation was created. It is historical baseline information, not a description of the current workspace.

### Findings

| Area | Current state |
|---|---|
| Workspace contents | Assignment and design documents, `.idea` metadata, and `README.zip`; the archive contains documentation only. |
| Application source | No Java or other application source files, domain model, service, repository, or API/CLI implementation found. |
| Build configuration | No `pom.xml`, Gradle files, or other build configuration found. Maven is not currently configured for the project and will be installed directly in the development environment; no Maven Wrapper will be added. |
| Tests | No test source, test framework configuration, or test commands found. |
| Technology evidence | IntelliJ metadata identifies a Java module and JDK 21 language level; Java 21 and `javac` are available in the current environment. No framework or persistence technology is identified. |
| Reusable application components | None found. Reuse the existing requirements, C4 diagrams, LLD, and ADRs as design references. |

The IDE metadata indicates Java 21. The finalized implementation uses Java 21, Maven, and JUnit 5, with Maven installed directly in the development environment and no Maven Wrapper. No web framework or database is needed.

## 2. Scope and implementation shape

Implement only the one-offering slice: enrollment, dropping, prerequisite checks, duplicate prevention, FIFO waitlisting, and promotion. Use one process and an in-memory repository. Keep the service, queue, and persistence responsibilities aligned with [the LLD](./architecture/lld.md); do not add a frontend, external services, a database, multiple offerings, or a configurable policy engine.

For the no-starter fallback, use a small CLI and single-process in-memory repository. Accept an integer capacity greater than or equal to zero; reject negative or invalid values. As no authoritative prerequisite data or rule exists, use a documented in-memory set of eligible learner IDs as a demo fixture. Keep this fixture separate from business logic; it is not a new prerequisite rule.

### Proposed structure

```text
pom.xml
README.md
docs/AI-USAGE-REPORT.md
src/
  main/
    java/com/example/coursewaitlist/
      Main.java
      application/
        EnrollmentService.java
        PromotionCoordinator.java
        PrerequisiteValidator.java
        WaitlistManager.java
        DemoPrerequisiteValidator.java
      domain/
        Offering.java
        Enrollment.java
        WaitlistEntry.java
        RegistrationResult.java
      persistence/
        RegistrationRepository.java
        InMemoryRegistrationRepository.java
  test/
    java/com/example/coursewaitlist/
      EnrollmentServiceTest.java
      PromotionCoordinatorTest.java
      RegistrationConcurrencyTest.java
      RegistrationIntegrationTest.java
```

`DemoPrerequisiteValidator` should read the in-memory eligible-ID set supplied by CLI/demo setup and answer eligibility checks; it must not own enrollment, capacity, or promotion policy. `Main` should provide only the basic CLI boundary and result display. Use the existing LLD roles where they clarify behavior; avoid adding DTOs, interfaces, or classes that the chosen implementation does not need. If the implementation uses fewer files while preserving the responsibilities and invariants, record the deviation rather than creating ceremony for its own sake.

## 3. Incremental implementation steps

The steps are ordered by dependency. Add or adapt focused tests with each behavior rather than postponing all validation until the end.

### Step 1 — Establish the runnable Java project

- **Dependencies:** None.
- **Files to create:** `pom.xml`, proposed Java package/source directories, initial test directory.
- **Files to modify:** `README.md` for the eventual build/run/test commands.
- **Expected functionality:** Configure Java 21 compilation and JUnit 5 tests. Maven is installed directly in the development environment; do not add Maven Wrapper. Do not add runtime libraries unless implementation requires them; configure the runnable JAR after the CLI entry point exists.
- **Tests and acceptance criteria:** Confirm the project compiles and a trivial test is discovered and passes; this is setup validation, not a feature acceptance test.
- **Completion criteria:** A clean checkout can run the test command and package the application classes using the documented Java/Maven prerequisites.

### Step 2 — Model synchronized offering and registration state

- **Dependencies:** Step 1.
- **Files to create:** `domain/Offering.java`, `domain/Enrollment.java`, `domain/WaitlistEntry.java`, and the smallest suitable `RegistrationResult`/outcome representation; `persistence/RegistrationRepository.java` and `InMemoryRegistrationRepository.java`.
- **Expected functionality:** Represent the single offering, capacity, active enrollments, and deterministic FIFO waitlist. Accept capacity `>= 0`; reject negative values. Enforce synchronization from this step onward: guard all state reads and mutations used in allocation so concurrent operations cannot exceed capacity or create duplicate active registrations. Provide ordered candidate traversal that can skip ineligible waiters without changing their positions. Prepare state changes so a failed multi-record operation can leave the prior state unchanged.
- **Tests and acceptance criteria:** Unit-test capacity zero, positive capacity, negative capacity rejection, queue order, duplicate active-state prevention, and state consistency. Add focused concurrency tests for repository/state transitions as synchronization is introduced. Cover AC-09 and the invariants later exercised by AC-11.
- **Completion criteria:** The synchronized in-memory state is deterministic; its operations provide the foundation for atomic enrollment allocation and atomic drop-plus-promotion, with no learner both enrolled and waitlisted.

### Step 3 — Implement eligibility, waitlist priority, and enrollment

- **Dependencies:** Step 2.
- **Files to create:** `application/PrerequisiteValidator.java`, `application/DemoPrerequisiteValidator.java`, `application/EnrollmentService.java`, `application/WaitlistManager.java`, `application/PromotionCoordinator.java`, and `EnrollmentServiceTest.java`.
- **Expected functionality:** Keep the documented eligible-learner-ID fixture separate from business logic; validation reads the fixture while enrollment and promotion services own policy. Return clear outcomes for invalid, ineligible, already-enrolled, and already-waitlisted requests. Within the synchronized, failure-safe allocation operation, consider existing waitlisted learners first in FIFO order, revalidate eligibility, skip and retain ineligible candidates at their original positions, and promote the earliest eligible candidate(s). Enroll the requester only if capacity remains afterward; otherwise append them once to the queue tail.
- **Tests and acceptance criteria:** Unit-test eligible and ineligible requests, available capacity, full-course waitlisting, duplicates, malformed inputs, fixture lookup, FIFO skip-and-retain, and priority scenarios LLD-AC-01 through LLD-AC-03 (AC-01 through AC-03, AC-05 through AC-07, AC-09).
- **Completion criteria:** Enrollment and available-seat priority are implemented together without a dependency cycle: Step 2 supplies synchronized state/queue primitives, and this step supplies the promotion coordinator used by enrollment. All paths preserve capacity, uniqueness, eligibility, and queue invariants.

### Step 4 — Implement drops and FIFO promotion

- **Dependencies:** Steps 2 and 3. Reuse the promotion coordinator and queue operations completed in Step 3; do not reimplement or defer them.
- **Files to modify:** `application/EnrollmentService.java` to add the drop workflow; `application/PromotionCoordinator.java` only if drop-path integration requires it.
- **Files to create:** `PromotionCoordinatorTest.java` for drop-triggered promotion and failure cases, unless covered in the existing promotion test class.
- **Expected functionality:** A valid drop and subsequent waitlist promotion form one atomic state transition. Stage or snapshot the complete affected state under the synchronization boundary and commit only when the drop/promotion workflow succeeds; on an exception or failed promotion write, preserve the pre-operation state with no partial drop, lost queue entry, duplicate registration, or false promotion result. Revalidate prerequisites immediately before promotion; skip and retain ineligible entries at their original positions. If no eligible learner exists, successfully complete the drop and leave the seat available.
- **Tests and acceptance criteria:** Unit and failure-handling tests for drop, eligible FIFO promotion, revalidation, skip-and-retain, no eligible candidate, empty queue, unknown/repeated drop, and injected failures at validation/enrollment/queue-update/commit points (AC-04 through AC-09). Verify rollback leaves state unchanged on failure and that successful no-candidate drop leaves a seat available.
- **Completion criteria:** Drop plus promotion commits atomically in memory; every injected failure leaves a valid, unchanged pre-operation state. The user-selected policies remain consistent with ADR-06/ADR-07 and the LLD.

### Step 5 — Add the minimal runnable CLI

- **Dependencies:** Steps 1 through 4.
- **Files to create:** `Main.java`.
- **Files to modify:** `pom.xml` to set the executable JAR entry point; `README.md` for the actual CLI and run instructions.
- **Expected functionality:** Start one offering with an integer capacity `>= 0`; reject negative or invalid values. Accept only the enrollment/drop operations needed for the assignment and display clear outcomes. Keep the documented in-memory set of eligible learner IDs in CLI/demo setup, separate from business logic. Do not add catalog, identity, persistence, or administrative product features.
- **Tests and acceptance criteria:** Integration-test CLI setup and a session covering enrollment, waitlisting, drop/promotion, eligibility fixture behavior, invalid input, and repeated drop; compare observed outcomes/state with AC-01 through AC-08.
- **Completion criteria:** The packaged application starts from documented commands and demonstrates the complete scoped workflow.

### Step 6 — Verify concurrency and cross-operation invariants

- **Dependencies:** Synchronization is implemented in Step 2; allocation/promotion behavior is implemented in Steps 3 and 4; CLI-level exercising is available after Step 5.
- **Files to create:** `RegistrationConcurrencyTest.java`; adjust only the implementation files that contain the atomic state transition if tests expose a defect.
- **Expected functionality:** Verify the synchronization already in place protects capacity checks, duplicate checks/writes, existing-waiter priority, FIFO selection, promotion, and drop-plus-promotion. Keep the single-process in-memory implementation simple; do not defer synchronization until this verification step.
- **Tests and acceptance criteria:** Concurrency-test distinct and duplicate eligible requests and overlapping drop/enrollment operations where practical. Assert no over-capacity, no duplicate active registration, no eligible-waiter bypass, and no partial state on failure (AC-09, AC-11, LLD-AC-04).
- **Completion criteria:** Unit, integration, and concurrency tests reliably pass; no distributed lock, database, or concurrency framework is introduced.

### Step 7 — Finalize evidence and handoff

- **Dependencies:** Steps 1 through 6.
- **Files to create:** `docs/AI-USAGE-REPORT.md`.
- **Files to modify:** `README.md`; update `architecture/lld.md` and/or `architecture/architecture-decisions.md` only where actual implementation details resolve an existing uncertainty or differ from the documented design.
- **Expected functionality:** Document actual setup, execution, test commands, limitations, and follow-up risks. Record prompts actually used, concise relevant response summaries, accepted/modified/rejected suggestions, affected files/decisions, and independent verification evidence. Do not fabricate prompts or verification results.
- **Tests and acceptance criteria:** Run the full focused suite and verify each documented command from a clean build. Check the implementation against the requirements, selected ADRs, and C4/LLD.
- **Completion criteria:** Deliver runnable code, automated tests, design specification, AI-use record, run instructions, and limitation summary (D-01 through D-06).

## 4. Domain models, services, repositories, and boundary

| Responsibility | Proposed type/file | Key behavior |
|---|---|---|
| Offering/capacity | `Offering` | Holds the sole offering's validated capacity. No catalog or term model. |
| Enrolled state | `Enrollment` | Represents one active seat for one learner. |
| Queue state | `WaitlistEntry` | Represents an eligible learner's stable FIFO position; skipped candidates keep their original position. |
| Outcome | `RegistrationResult` | Represents clear success and expected business outcomes, including enrolled, waitlisted, ineligible, duplicate, dropped, not enrolled, and invalid request. Exact messages are implementation-specific. |
| Prerequisites | `PrerequisiteValidator` and demo fixture | Checks eligibility for new enrollment and rechecks immediately before promotion. The documented in-memory eligible-ID set is supplied by CLI/demo setup and remains separate from business policy. |
| Enrollment/drop use case | `EnrollmentService` | Coordinates validation, duplicate checks, available-seat priority, enrollment/waitlist changes, drops, and promotion. |
| Promotion | `PromotionCoordinator` | Scans FIFO candidates, skips-but-retains ineligible entries, promotes the earliest eligible candidate, and stops when no seat or eligible candidate remains. |
| Queue operations | `WaitlistManager` | Adds eligible entries at the tail and exposes deterministic ordered candidates; removes entries only on successful promotion. |
| State boundary | `RegistrationRepository` / `InMemoryRegistrationRepository` | Reads and mutates capacity, enrollments, and waitlist state as one in-memory store. No database is proposed. |
| User interaction | `Main` | Parses the minimal CLI interaction and displays service outcomes; contains no business rules. |

### Required state invariants

1. `0 <= enrolledCount <= capacity`.
2. A learner has at most one active registration and cannot be enrolled and waitlisted simultaneously.
3. Only eligible learners can be enrolled or initially waitlisted.
4. Promotion revalidates prerequisites, preserves skipped ineligible entries at their original queue positions, and removes an entry only when its enrollment succeeds.
5. Queue order is deterministic FIFO.
6. Existing eligible waitlisted learners are considered before a new eligible learner receives an available seat (ADR-06 and LLD-AC-01 through LLD-AC-04).
7. Invalid, duplicate, or repeated requests do not corrupt unrelated registration state.

## 5. Transaction and concurrency approach

- **Starter compatibility:** No starter transaction or locking mechanism exists in the inspected workspace. The selected no-starter implementation uses a single-process in-memory repository.
- **Synchronization from the start:** Introduce one synchronization boundary with the state store in Step 2. Hold it across complete state-changing workflows: duplicate check, eligibility evaluation, available-seat priority allocation, enrollment or waitlist insertion, drop, and promotion. This prevents over-capacity, duplicate active registrations, and bypass of eligible existing waiters.
- **Atomic state changes:** Perform business decisions against staged state (or an equivalent rollback-safe snapshot) and publish the resulting state as one commit while holding the synchronization boundary. Drop plus promotion is one atomic operation. If eligibility evaluation fails technically, a state mutation fails, or commit preparation fails, retain the prior state unchanged; do not report an uncommitted drop or promotion. An ineligible candidate is an expected policy outcome, not a technical failure: retain and skip that entry, and commit a successful drop with the seat available if no eligible waiter remains.
- **Failure handling:** Return expected business outcomes explicitly. Surface unexpected failures; do not convert them to success or silently ignore them. Ensure errors during enrollment-side priority allocation likewise leave no partial state.
- **Concurrency tests:** Exercise overlapping eligible requests, repeated learner requests, and concurrent drop/enrollment operations. Assert exact invariants after completion; do not depend on nondeterministic thread scheduling to define FIFO arrival order.

## 6. Prerequisite validation and capacity configuration

- **Capacity:** Accept any integer capacity `>= 0`; reject negative, malformed, or missing values with a clear input error. Do not silently substitute a default.
- **Prerequisite rule/data:** The assignment requires eligibility checks but provides no course rule or learner records. Use a documented in-memory set of eligible learner IDs for the demo. Supply this fixture separately from business logic; the validator consults it, while enrollment and promotion services apply the selected policies. This is demo eligibility data, not an invented prerequisite rule.
- **Validation timing:** Check eligibility before enrollment or initial waitlisting, and revalidate each candidate immediately before promotion. If the eligibility source cannot be read, do not enroll or promote on an assumed success.
- **Policy scope:** Do not build a configurable prerequisite or waitlist policy engine. Implement the finalized fixed behavior: FIFO; skip and retain ineligible waitlisted learners; serve existing eligible waiters before new enrollment requests; leave the seat open after a drop if no eligible waiter is found.

## 7. Test plan and requirement-to-test traceability

Use JUnit 5 unit, integration, boundary, negative, failure-handling, and concurrency tests. Test domain/services and the CLI/in-memory wiring; assert both returned outcomes and final state. Exercise injected failures to verify rollback and absence of partial state corruption.

| Requirement / acceptance | Planned test evidence |
|---|---|
| FR-01; AC-01 | Eligible learner enrolls in the one offering when capacity remains; assert exactly one enrollment. |
| FR-02, FR-03; AC-01, AC-06 | Eligible learner is accepted; ineligible learner is neither enrolled nor waitlisted. |
| FR-04; AC-02 | At capacity, eligible learner joins once at the FIFO tail and enrolled count remains within capacity. |
| FR-05; AC-03 | Repeat requests for enrolled and waitlisted learners; assert clear duplicate outcome and one active record only. |
| FR-06; AC-04, AC-08 | Valid drop releases a seat; unknown and repeated drops return clear outcomes without a second mutation. |
| FR-07; AC-04, AC-10 | Multiple eligible waiters are promoted in deterministic FIFO order after a seat opens. |
| FR-08; AC-05, AC-06 | Ineligible queue head is skipped and retained; later eligible candidate is promoted; prerequisite is rechecked at promotion. |
| FR-09; AC-07 through AC-09 | Invalid enrollment/drop requests, duplicates, and repeated drops preserve capacity, registrations, and queue state. |
| ADR-06; LLD-AC-01 | With one open seat and an eligible waiter, an incoming eligible learner does not bypass the waiter. |
| ADR-06; LLD-AC-02 | Ineligible waiters are skipped without movement/removal; earliest later eligible waiter gets priority. |
| ADR-06; LLD-AC-03 | If all existing waiters are ineligible, their positions remain and the new eligible learner may take the open seat. |
| NFR-01; AC-09 | After success, rejection, duplicate, drop, and promotion operations, assert `0 <= enrolled <= capacity` and no duplicate active learners. |
| NFR-02, NFR-03; AC-11; LLD-AC-04 | Race distinct and duplicate requests and overlapping drop/enrollment operations; assert no over-capacity, duplicate active state, eligible-waiter bypass, or partial state. |
| D-04 | Review `docs/AI-USAGE-REPORT.md` for actual prompts, dispositions, affected decisions/files, and independently run verification evidence. |

### Boundary and negative cases

- Capacity zero, positive capacity, exact last available seat, full capacity, and negative/malformed/missing capacity arguments.
- Malformed learner identifiers and invalid capacity configuration.
- Duplicate enrollment while enrolled and while waitlisted.
- Waitlist empty; one candidate; multiple candidates; ineligible first and middle entries.
- Eligibility changes between initial waitlisting and promotion.
- No eligible candidate after a drop; drop completes and seat remains available.
- Unknown learner drop and repeated drop.
- Injected eligibility, promotion-write, and commit-preparation failures; verify drop/promotion and enrollment allocation leave prior state unchanged.
- State remains consistent after each rejected request and each completed transition.

## 8. Build, run, and test verification

No build or test command currently exists. Install Maven directly in the development environment (do not add Maven Wrapper), then add the Maven/JUnit 5 configuration and document exact commands in `README.md`. With JDK 21 and Maven installed, the expected verification sequence is:

```text
mvn clean test
mvn package
java -jar target/course-registration-waitlist-1.0.0.jar --capacity 2
```

The final CLI arguments and interaction syntax must match the implemented interface; adjust the README command to that actual interface. Verify a clean build, all tests, a representative CLI session for enrollment/waitlisting/drop/promotion, and the concurrency test. Record actual outputs/results in the AI-use and handoff evidence where relevant; do not claim tests passed unless run.

## 9. Remaining implementation decisions

| Question | Recommended simplest approach | Information needed |
|---|---|---|
| What real course prerequisite rule and authoritative learner data apply beyond the prototype? | Use the finalized documented in-memory set of eligible learner IDs as the demo fixture, separately from business logic; do not invent a course rule. | A real prerequisite rule/data source is needed only if the prototype is later expected to represent actual learner eligibility. |
| What CLI syntax and sample eligible IDs should be documented? | Use a small command loop for `enroll`, `drop`, and `quit`, and list sample eligible IDs in the README or CLI help. | Choose concrete sample identifiers and input syntax during implementation; neither blocks the selected design. |
| What are invalid learner IDs and exact outcomes? | Define minimal nonblank validation and clear result categories; keep messages stable enough for tests. | Any starter-specific identifier format or response conventions. |

There are no blocking design decisions for the assignment-sized prototype: Java 21, Maven/JUnit 5, CLI, single-process in-memory storage, demo eligibility fixture, and capacity `>= 0` are selected. Maven must be installed directly before running the build. A real prerequisite rule/data source remains outside this demo fixture and is needed only if the prototype must represent actual course eligibility. Final CLI syntax and sample IDs are implementation details.

The waitlist policy, eligibility revalidation, priority for existing eligible waiters, skip-and-retain behavior, no-eligible-candidate seat behavior, duplicate/capacity invariants, atomic drop-plus-promotion, and single-application scope are finalized in ADR-01 through ADR-09 and should not be reopened during implementation absent contradictory starter constraints. The assignment's suggested 7–8 hour timebox is guidance, not a requirement; prioritize core invariants and focused tests within it.
