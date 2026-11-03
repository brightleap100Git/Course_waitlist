# Verification Report — Course Registration and Waitlist Management

## 1. Executive summary

**Recommendation: CONDITIONAL GO.** The current implementation is buildable, the complete automated suite passes, coverage targets are met, and CLI behavior is supported by scripted testing plus user-provided command-prompt evidence. An [AI usage report](./AI-USAGE-REPORT.md) now records available prompts, checkpoint summaries, artifacts, and verification evidence. Because raw historic prompts/responses and suggestion dispositions are unavailable, AC-12 remains only partially demonstrated.

| Area | Status | Evidence |
|---|---|---|
| Clean build and automated tests | **PASS** | `mvn clean verify`: 64 tests, 0 failures, 0 errors, 0 skipped. |
| JaCoCo quality gates | **PASS** | Overall line coverage 99.42%, branch coverage 96.97%; business/domain branch coverage 95.24%. |
| Requirement behavior | **PASS**, except AC-12 | FR/BR/AC traceability matrix below. |
| Scripted CLI verification | **PASS** | Runnable JAR driven through separate scripted stdin/stdout sessions. |
| Human-interactive CLI check | **PASS (user-provided evidence)** | User-provided command-prompt transcript is recorded in [MANUAL-VERIFICATION-REPORT.md](./MANUAL-VERIFICATION-REPORT.md). It was not independently captured by the assistant. |
| AI-use record, AC-12 | **PARTIAL** | [AI-USAGE-REPORT.md](./AI-USAGE-REPORT.md) records accessible evidence; historic prompts/responses and per-suggestion dispositions remain unavailable. |

This report distinguishes **PASS** (executed and verified), **FAIL** (executed and did not meet the stated criterion), **PENDING** (not executed), and **NOT APPLICABLE** (outside the assignment scope).

## 2. Environment, source revision, and build metadata

Captured before report changes:

| Item | Observed value |
|---|---|
| Git revision | `d7c3d18bca58bb3320b01512b8f214a613e8a339` |
| Branch/tracking | `main...origin/main` |
| Initial working tree | Dirty. `README.md` was modified; `.idea/`, `architecture/`, `codebase.zip`, `implementation-plan.md`, `pom.xml`, `requirement.md`, `src/`, `target/`, and `test-plan.md` were untracked. These were observed before this report's changes and were not reverted. |
| OS | Microsoft Windows 11 Home Single Language, version `10.0.26200`, build `26200`, amd64 |
| Java | Oracle Java SE `21.0.3` LTS |
| Maven | Apache Maven `3.9.16` |
| Project | `com.example:course-registration-waitlist:1.0-SNAPSHOT`; Maven compiler release 21 |
| Persistence/demo | One offering, in-memory synchronized repository, capacity 2, eligible demo IDs `eligible-1` through `eligible-4` |

The baseline command, run before report/README edits, was `mvn clean verify`. It succeeded with **64 tests, 0 failures, 0 errors, 0 skipped** and reported that all JaCoCo checks were met. This matches the preceding 64-test result; no Java implementation changes were needed for this verification step.

Reproduce from the project root (Maven is installed directly; no wrapper is used):

```powershell
mvn compile
mvn test
mvn clean verify
mvn package
java -jar target\course-registration-waitlist-1.0-SNAPSHOT.jar
```

The final `mvn clean verify` after documentation changes is recorded in section 10. JaCoCo HTML/XML and Surefire XML/text artifacts are under `target\site\jacoco\` and `target\surefire-reports\`.

## 3. Automated test execution

### Full-suite result

Final clean verification ran the following Surefire test classes:

| JUnit class | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `com.example.coursewaitlist.application.DemoPrerequisiteValidatorTest` | 3 | 0 | 0 | 0 |
| `com.example.coursewaitlist.application.EnrollmentServiceTest` | 27 | 0 | 0 | 0 |
| `com.example.coursewaitlist.cli.RegistrationCliTest` | 13 | 0 | 0 | 0 |
| `com.example.coursewaitlist.domain.DomainModelsTest` | 7 | 0 | 0 | 0 |
| `com.example.coursewaitlist.persistence.InMemoryRegistrationRepositoryTest` | 13 | 0 | 0 | 0 |
| `com.example.coursewaitlist.ProjectSetupTest` | 1 | 0 | 0 | 0 |
| **Total unique test cases** | **64** | **0** | **0** | **0** |

The full suite ran once for the final evidence. Focused Surefire reruns below are additional executions of existing tests, not additional unique test cases.

### Focused Surefire runs

| Command | Exact test methods/classes selected | Assertion purpose | Actual result |
|---|---|---|---|
| `mvn '-Dtest=EnrollmentServiceTest#testTP01EnrollsAnEligibleLearnerWhenCapacityRemains+testTP02AndTP06WaitlistsEligibleLearnerWhenCourseIsFull+testTP05AndTP36RejectsIneligibleLearnerWithoutMutation+testTP03ReturnsAlreadyEnrolledWithoutChangingState+testTP04ReturnsAlreadyWaitlistedWithoutChangingQueuePosition+testTP14PromotesEarliestEligibleWaiterAfterDrop+testTP15TP17TP18RevalidatesAndRetainsIneligibleFrontWaiter+testTP19RechecksPreviouslyIneligibleWaiterAtLaterAllocationOpportunity' test` | EnrollmentService methods named in selector | Enrollment, waitlisting, prerequisite failure, duplicates, FIFO promotion, revalidation, and priority | **PASS**; Maven reported 7 tests, 0 failures/errors/skips. |
| `mvn '-Dtest=EnrollmentServiceTest#testTP11DropsEnrolledLearnerWhenWaitlistIsEmpty+testTP12AndTP13UnknownOrRepeatedDropReturnsNotEnrolledWithoutMutation+testTP12DroppingWaitlistedLearnerDoesNotTriggerPromotion+testTP23TP24AndTP26PromotionFailureRollsBackDropAndEarlierPromotion+testTP32ConcurrentDropAndEnrollmentPreserveWaitlistPriorityAndCapacity+testTP34ConcurrentDropsPromoteWaitersInFifoOrderWithoutDuplicates+testTP35ConcurrentRepeatedDropsReleaseOnlyOneSeatAndPromoteOnce+testTP31AndTP32ConcurrentEnrollmentPreservesCapacityAndUniqueness+testTP25RollsBackWaitlistAllocationsIfLaterEligibilityCheckFails' test` | EnrollmentService methods named in selector | Drop outcomes, atomic rollback after injected failure, promotion, and concurrent enrollment/drop behavior | **PASS**; 9 tests, 0 failures/errors/skips. |
| `mvn '-Dtest=InMemoryRegistrationRepositoryTest#simultaneousAttemptsCannotCreateDuplicateActiveRegistrations+testTP31AndTP32SerializesConcurrentEnrollmentWrites+testAC09TransactionCanReadStateWithoutExposingMutableCollections' test` | Three `InMemoryRegistrationRepositoryTest` methods | Concurrent duplicate prevention, serialized writes, immutable read view | **PASS**; 3 tests, 0 failures/errors/skips. |
| `mvn -Dtest=RegistrationCliTest test` | All `RegistrationCliTest` methods | CLI wiring, parsing, output, state view, session persistence, EOF and exit behavior | **PASS**; 13 tests, 0 failures/errors/skips. |

### Important behavior and test evidence

| Scenario | JUnit test class and method(s) | Test-plan IDs | Actual result |
|---|---|---|---|
| Successful enrollment | `EnrollmentServiceTest.testTP01EnrollsAnEligibleLearnerWhenCapacityRemains`; `RegistrationCliTest.testTP01EnrollsAnEligibleLearnerThroughTheService` | TP-01 | **PASS** |
| Full-course waitlisting and zero capacity | `EnrollmentServiceTest.testTP02AndTP06WaitlistsEligibleLearnerWhenCourseIsFull`; `EnrollmentServiceTest.testTP07QueuesEligibleLearnerWhenCapacityIsZero` | TP-02, TP-06, TP-07 | **PASS** |
| Prerequisite rejection | `EnrollmentServiceTest.testTP05AndTP36RejectsIneligibleLearnerWithoutMutation`; `DemoPrerequisiteValidatorTest.testTP05RejectsIdsMissingFromDemoEligibilityFixture` | TP-05, TP-36 | **PASS** |
| Duplicate active enrollment/waitlist | `EnrollmentServiceTest.testTP03ReturnsAlreadyEnrolledWithoutChangingState`; `EnrollmentServiceTest.testTP04ReturnsAlreadyWaitlistedWithoutChangingQueuePosition`; `InMemoryRegistrationRepositoryTest.simultaneousAttemptsCannotCreateDuplicateActiveRegistrations` | TP-03, TP-04, TP-31 | **PASS** |
| FIFO promotion, skip-and-retain, and revalidation | `EnrollmentServiceTest.testTP14PromotesEarliestEligibleWaiterAfterDrop`; `EnrollmentServiceTest.testTP15TP17TP18RevalidatesAndRetainsIneligibleFrontWaiter`; `EnrollmentServiceTest.testTP19RechecksPreviouslyIneligibleWaiterAtLaterAllocationOpportunity` | TP-14 through TP-19 | **PASS** |
| Existing-waiter priority over a newcomer | `EnrollmentServiceTest.testLLDAC01AllocatesOpenSeatToExistingEligibleWaiterBeforeNewcomer`; `testLLDAC02SkipsAndRetainsIneligibleWaiterBeforePromotingNextEligible`; `testLLDAC03EnrollsNewcomerWhenAllExistingWaitersAreIneligible` | TP-19 through TP-21; LLD-AC-01 through LLD-AC-03 | **PASS** |
| Drop, automatic promotion, repeated/invalid drop | `EnrollmentServiceTest.testTP11DropsEnrolledLearnerWhenWaitlistIsEmpty`; `testTP12AndTP13UnknownOrRepeatedDropReturnsNotEnrolledWithoutMutation`; `testTP10ReturnsInvalidRequestForNullDropLearnerId` | TP-10 through TP-14 | **PASS** |
| Atomic rollback on validator failure | `EnrollmentServiceTest.testTP23TP24AndTP26PromotionFailureRollsBackDropAndEarlierPromotion`; `testTP25RollsBackWaitlistAllocationsIfLaterEligibilityCheckFails` | TP-23 through TP-26 | **PASS** |
| Concurrency: enrollment, duplicate requests, drops/promotions | `EnrollmentServiceTest.testTP31AndTP32ConcurrentEnrollmentPreservesCapacityAndUniqueness`; `testTP32ConcurrentDropAndEnrollmentPreserveWaitlistPriorityAndCapacity`; `testTP34ConcurrentDropsPromoteWaitersInFifoOrderWithoutDuplicates`; `testTP35ConcurrentRepeatedDropsReleaseOnlyOneSeatAndPromoteOnce`; repository methods listed in focused run above | TP-31 through TP-35 | **PASS** |
| CLI parsing, state output, ordered queue, workflows | `RegistrationCliTest` (13 methods; see Surefire XML) | TP-01 through TP-05, TP-10, TP-12 through TP-14, TP-29 plus additional interface tests | **PASS** |

Surefire XML records each test method and suite totals in `target\surefire-reports\TEST-*.xml`; text summaries are `target\surefire-reports\*.txt`.

## 4. JaCoCo coverage

JaCoCo 0.8.13 generated `target\site\jacoco\index.html` and `target\site\jacoco\jacoco.xml`. Counts below are instructions/lines only where explicitly labelled; percentages use JaCoCo line and branch counters. Interfaces without executable source report `N/A`.

### Overall and package coverage

| Scope | Lines covered / total | Line coverage | Branches covered / total | Branch coverage |
|---|---:|---:|---:|---:|
| Overall | 342 / 344 | **99.42%** | 96 / 99 | **96.97%** |
| Domain package | 58 / 58 | 100.00% | 12 / 12 | 100.00% |
| Application/business package | 83 / 85 | 97.65% | 28 / 30 | 93.33% |
| **Domain + application (business/domain)** | **141 / 143** | **98.60%** | **40 / 42** | **95.24%** |
| Persistence package | 110 / 110 | 100.00% | 16 / 16 | 100.00% |
| CLI package | 89 / 89 | **100.00%** | 40 / 41 | **97.56%** |
| Main entry-point package | 2 / 2 | 100.00% | 0 / 0 | N/A |

Business/domain exceeds the required 90% line and 85% branch targets. CLI is reported separately. No JaCoCo class exclusions were added.

### Per-class coverage

| Class | Lines | Branches |
|---|---:|---:|
| `cli.RegistrationCli` | 77/77 (100%) | 40/41 (97.56%) |
| `cli.DemoConfiguration` | 12/12 (100%) | N/A |
| `Main` | 2/2 (100%) | N/A |
| `application.EnrollmentService` | 42/43 (97.67%) | 17/18 (94.44%) |
| `application.PromotionCoordinator` | 24/24 (100%) | 6/6 (100%) |
| `application.WaitlistManager` | 11/12 (91.67%) | 3/4 (75%) |
| `application.DemoPrerequisiteValidator` | 6/6 (100%) | 2/2 (100%) |
| `application.PrerequisiteValidator` | N/A (interface) | N/A |
| `persistence.InMemoryRegistrationRepository` | 17/17 (100%) | 4/4 (100%) |
| `persistence.InMemoryRegistrationRepository.WorkingState` | 10/10 (100%) | N/A |
| `persistence.InMemoryRegistrationRepository.State` | 13/13 (100%) | N/A |
| `persistence.InMemoryRegistrationRepository.MutableTransaction` | 64/64 (100%) | 12/12 (100%) |
| `persistence.RegistrationSnapshot` | 6/6 (100%) | N/A |
| `persistence.RegistrationRepository` and nested `Transaction` | N/A (interfaces) | N/A |
| `domain.LearnerId` | 5/5 (100%) | 2/2 (100%) |
| `domain.Offering` | 4/4 (100%) | 2/2 (100%) |
| `domain.EnrollmentId` | 4/4 (100%) | 2/2 (100%) |
| `domain.WaitlistEntryId` | 4/4 (100%) | 2/2 (100%) |
| `domain.WaitlistEntry` | 6/6 (100%) | 2/2 (100%) |
| `domain.ActiveRegistration` | 6/6 (100%) | 2/2 (100%) |
| `domain.EligibilityData` | 4/4 (100%) | N/A |
| `domain.EligibilityResult` | 3/3 (100%) | N/A |
| `domain.Enrollment` | 4/4 (100%) | N/A |
| `domain.RegistrationResult` | 6/6 (100%) | N/A |
| `domain.RegistrationOutcome` | 9/9 (100%) | N/A |
| `domain.RegistrationKind` | 3/3 (100%) | N/A |

### Missed lines and branches

- `EnrollmentService.java`: line 80 (`removeEnrollment` unexpectedly returns false after the active record was found); one branch at line 79. This stale-record path is guarded inside the same repository transaction and is not reachable with the concrete synchronized repository absent a repository defect. The failure is surfaced, not silently converted to success.
- `WaitlistManager.java`: line 30 and one branch at line 29 (defensive exception if asked to remove an entry that no longer exists). The coordinator selects and removes within one transaction; the concrete repository serializes that operation. This remains a defensive failure path.
- `RegistrationCli.java`: one branch at line 81, the compiler-generated fallback of the exhaustive `RegistrationOutcome` enum switch. All current enum outcomes are individually covered; the missing branch is not a current business outcome.

No behavior affecting the validated required workflows is uncovered. The two defensive repository inconsistency paths are not excluded and do not cause the business/domain thresholds to fail.

### Comparison with the previous 64-test baseline

The previous Step 5 report recorded 64 tests and rounded coverage of 99% lines / 96% branches overall, 100% lines / 97% branches for CLI. The fresh run again has 64 tests. Current exact values are 99.42% / 96.97% overall and 100.00% / 97.56% for the CLI package. The prior report did not preserve its raw JaCoCo counters in a versioned artifact, so exact counter-to-counter deltas cannot be established; displayed differences are consistent with percentage rounding and test-only additions. No business implementation changes were made for this verification step.

## 5. Requirement-to-test traceability

`PASS` here means execution evidence exists in the current full suite or the stated manual/document audit. Test-plan IDs are from `test-plan.md`; test method names are actual source names. The additional CLI tests are interface verification and do not introduce new assignment requirement IDs.

### Functional requirements

| ID | Short description | Test-plan IDs | Actual JUnit method(s) / verification | Result and evidence | Coverage gap |
|---|---|---|---|---|---|
| FR-01 | One-offering enrollment | TP-01, TP-29 | `EnrollmentServiceTest.testTP01EnrollsAnEligibleLearnerWhenCapacityRemains`; `RegistrationCliTest.testTP01EnrollsAnEligibleLearnerThroughTheService`; CLI session B | **PASS**; Surefire XML and scripted JAR output | None |
| FR-02 | Enroll eligible learner when capacity remains, after selected waiter priority | TP-01, TP-19 through TP-21, TP-32/33 | `EnrollmentServiceTest.testTP01...`; `testLLDAC01...`; `testLLDAC03...`; concurrent allocation test | **PASS**; service tests and focused run | Priority is selected ADR-06 policy, not changed requirement text |
| FR-03 | Never enroll ineligible learner | TP-05, TP-36 | `EnrollmentServiceTest.testTP05AndTP36RejectsIneligibleLearnerWithoutMutation`; `DemoPrerequisiteValidatorTest.testTP05RejectsIdsMissingFromDemoEligibilityFixture` | **PASS** | Demo rule only; real prerequisites are outside available data |
| FR-04 | Eligible learner may wait when full | TP-02, TP-06, TP-07, TP-30, TP-32 | `EnrollmentServiceTest.testTP02AndTP06WaitlistsEligibleLearnerWhenCourseIsFull`; `testTP07QueuesEligibleLearnerWhenCapacityIsZero`; CLI full-course test | **PASS** | None |
| FR-05 | Prevent duplicate enrollment/waitlist records | TP-03, TP-04, TP-31, TP-32 | `EnrollmentServiceTest.testTP03ReturnsAlreadyEnrolledWithoutChangingState`; `testTP04ReturnsAlreadyWaitlistedWithoutChangingQueuePosition`; `InMemoryRegistrationRepositoryTest.simultaneousAttemptsCannotCreateDuplicateActiveRegistrations` | **PASS** | None |
| FR-06 | Drop enrollment and release seat | TP-11, TP-14 through TP-18, TP-28, TP-29, TP-34 | `EnrollmentServiceTest.testTP11DropsEnrolledLearnerWhenWaitlistIsEmpty`; `testTP14PromotesEarliestEligibleWaiterAfterDrop`; concurrent drop tests | **PASS** | None |
| FR-07 | FIFO promotion of earliest eligible waiter | TP-14 through TP-22, TP-29, TP-34 | `EnrollmentServiceTest.testTP14PromotesEarliestEligibleWaiterAfterDrop`; `testTP14AndTP22PromoteWaitlistedLearnersInFifoOrderWhileCapacityRemains`; `testTP15TP17TP18RevalidatesAndRetainsIneligibleFrontWaiter` | **PASS** | None |
| FR-08 | Do not promote ineligible learner; apply documented policy | TP-15 through TP-18, TP-20, TP-21, TP-26 | `EnrollmentServiceTest.testTP15TP17TP18RevalidatesAndRetainsIneligibleFrontWaiter`; `testTP16TP17TP28LeaveSeatAvailableWhenEveryWaiterFailsRevalidation`; `testTP19RechecksPreviouslyIneligibleWaiterAtLaterAllocationOpportunity` | **PASS** | Selected policy is skip-and-retain per ADR-06 |
| FR-09 | Clear invalid/repeated request outcomes; preserve state | TP-08 through TP-13, TP-23 through TP-27, TP-30, TP-35 | Enrollment service invalid/drop tests; `RegistrationCliTest.testTP10TP12ReportsMissingLearnerAndNotEnrolled`; `testTP13ReportsRepeatedDropAfterSuccessfulDrop`; rollback tests | **PASS** | CLI-specific whitespace identifier syntax is limited to tokenized text commands |
| FR-10 | Reuse starter or minimal Java/Maven/CLI/in-memory fallback | N/A (manual review) | Inspected project, Java/Maven configuration, `Main`, `DemoConfiguration`, repository, and runnable JAR | **PASS** for fallback: no starter implementation is present; configured one-offering Java 21/Maven CLI with in-memory storage | No durable persistence by design |

### Business rules

| ID | Short description | Test-plan IDs | Actual JUnit method(s) / verification | Result and evidence | Coverage gap |
|---|---|---|---|---|---|
| BR-01 | One enrollment per learner | TP-03, TP-04, TP-31/32 | `testTP03ReturnsAlreadyEnrolledWithoutChangingState`; `testTP04ReturnsAlreadyWaitlistedWithoutChangingQueuePosition`; repository concurrent duplicate test | **PASS** | None |
| BR-02 | Enrollment needs eligibility and capacity (after priority) | TP-01, TP-05, TP-19 through TP-21 | `testTP01...`; `testTP05AndTP36...`; `testLLDAC01...` through `testLLDAC03...` | **PASS** | None |
| BR-03 | Full eligible learner can wait | TP-02, TP-06, TP-07 | `testTP02AndTP06...`; `testTP07QueuesEligibleLearnerWhenCapacityIsZero` | **PASS** | None |
| BR-04 | Deterministic FIFO priority | TP-14, TP-15, TP-18, TP-22 | FIFO service tests; `InMemoryRegistrationRepositoryTest.testTP02AndTP04AppendsWaitlistEntriesInFifoOrderAndRejectsDuplicate`; CLI FIFO view test | **PASS** | None |
| BR-05 | Ineligible promotion must not enroll; queue policy documented | TP-15 through TP-18 | `testTP15TP17TP18RevalidatesAndRetainsIneligibleFrontWaiter`; `testTP16TP17TP28LeaveSeatAvailableWhenEveryWaiterFailsRevalidation` | **PASS** against ADR-06 (skip and retain) | Original requirement leaves choice open; implementation decision is documented |
| BR-06 | Drop frees seat for promotion | TP-11, TP-14, TP-28 | `testTP11DropsEnrolledLearnerWhenWaitlistIsEmpty`; `testTP14PromotesEarliestEligibleWaiterAfterDrop` | **PASS** | None |
| BR-07 | Invalid/duplicate/repeated requests preserve state | TP-03, TP-04, TP-10 through TP-13, TP-23 through TP-27 | service invalid/duplicate/drop and rollback tests; CLI validation tests | **PASS** | None |

### Acceptance criteria

| ID | Short description | Test-plan IDs | Actual JUnit method(s) / verification | Result and evidence | Coverage gap |
|---|---|---|---|---|---|
| AC-01 | Eligible learner enrolls into free capacity | TP-01, TP-19, TP-21, TP-32/33 | `EnrollmentServiceTest.testTP01EnrollsAnEligibleLearnerWhenCapacityRemains`; LLD priority tests | **PASS** |
| AC-02 | Full eligible learner is waitlisted | TP-02, TP-06, TP-07, TP-30, TP-32 | `EnrollmentServiceTest.testTP02AndTP06WaitlistsEligibleLearnerWhenCourseIsFull`; zero-capacity and CLI tests | **PASS** |
| AC-03 | Repeated enrollment does not duplicate records | TP-03, TP-04, TP-31/32 | `testTP03...`; `testTP04...`; concurrent duplicate test | **PASS** |
| AC-04 | Drop releases seat and promotes earliest eligible | TP-11, TP-14 through TP-20, TP-28/29/34 | `testTP14PromotesEarliestEligibleWaiterAfterDrop`; FIFO and CLI workflow tests | **PASS** |
| AC-05 | Ineligible learner not enrolled; selected queue policy applied | TP-05, TP-15 through TP-18, TP-20/21/26/36 | ineligible enrollment and skip/retain/revalidation tests | **PASS** |
| AC-06 | Learner failing prerequisite cannot enroll/promote | TP-05, TP-15 through TP-18, TP-26/36 | `testTP05AndTP36RejectsIneligibleLearnerWithoutMutation`; revalidation test | **PASS** |
| AC-07 | Invalid requests leave state consistent | TP-08 through TP-10, TP-23 through TP-27, TP-30 | invalid/null service tests, CLI malformed input tests, rollback tests | **PASS** |
| AC-08 | Unknown/repeated drop returns clear result without corruption | TP-12, TP-13, TP-30, TP-35 | `testTP12AndTP13UnknownOrRepeatedDropReturnsNotEnrolledWithoutMutation`; CLI repeated-drop test; concurrent repeated drop | **PASS** |
| AC-09 | Capacity, uniqueness, disjoint states, queue invariants hold | TP-01 through TP-36 as applicable | `EnrollmentServiceTest.assertInvariants` used in state-changing cases; repository snapshot/transaction tests | **PASS** |
| AC-10 | Focused automated tests cover required behaviors | TP-01 through TP-06, TP-14/15/18/29 | 64-test full suite and CLI integration suite | **PASS** |
| AC-11 | Preserve synchronization and add practical concurrency tests | TP-31 through TP-35 | repository and enrollment concurrency tests, including latches/executors | **PASS** |
| AC-12 | Record AI prompts, response summaries, dispositions, affected artifacts, verification | Manual deliverable review | [AI-USAGE-REPORT.md](./AI-USAGE-REPORT.md), current conversation excerpts, checkpoint summaries, and fresh Maven/JaCoCo evidence | **PARTIAL**; the report records available evidence, but raw historic prompts/responses and suggestion dispositions are not accessible |

## 6. Architecture and invariant verification

| Invariant/design decision | Evidence | Result |
|---|---|---|
| Enrolled count never exceeds configured capacity | `Offering`, repository `saveEnrollment`, service tests and repository concurrency tests; LLD §5 and §7; ADR-03 | **PASS** |
| A learner has at most one active registration and cannot be enrolled and waitlisted simultaneously | Repository active-registration index, guarded writes, duplicate and concurrent duplicate tests; LLD §5; ADR-04 | **PASS** |
| FIFO queue order is deterministic | Position sequence in repository, ordered queue tests and CLI `VIEW`; LLD §5; ADR-05 | **PASS** |
| Skipped ineligible waiters retain their original positions and relative order | Promotion coordinator continues scanning without removal; TP-15/17/18 tests; LLD §6; ADR-06 | **PASS** |
| Eligibility is rechecked at promotion time | `PromotionCoordinator.allocateAvailableSeats`; eligibility-change tests; LLD §4/§6; ADR-06/09 | **PASS** |
| Existing eligible waiters are allocated before new requests | Enrollment service invokes the existing coordinator before allocating to the requester; LLD focused acceptance scenarios; ADR-06 | **PASS** |
| Drop plus all triggered promotions are atomic | One `RegistrationRepository.transact` callback; copy-on-write rollback test injects a later eligibility failure; LLD §7; ADR-07 | **PASS** |
| CLI delegates business decisions to services | `RegistrationCli` only parses/displays and calls `EnrollmentService`; `DemoConfiguration` assembles dependencies. No CLI capacity, eligibility, or promotion policy is implemented. | **PASS** |
| Repository reads do not expose mutable state | `RegistrationSnapshot` copies collections; repository snapshot test; LLD §5 | **PASS** |
| One-offering, in-memory scope is preserved | `DemoConfiguration` uses one `Offering`, fixture IDs, and `InMemoryRegistrationRepository`; ADR-01/02 | **PASS** |

Additional selected-policy traceability: LLD-AC-01 through LLD-AC-04 have test-plan coverage TP-19 through TP-21 and TP-31 through TP-34. No contradiction with the C4/LLD design or ADR-03 through ADR-09 was found in the inspected runtime behavior.

## 7. Scripted CLI verification

The scenarios below are **scripted stdin/stdout smoke tests of the executable JAR**. They are supplemented by the user's interactive command-prompt transcript in [MANUAL-VERIFICATION-REPORT.md](./MANUAL-VERIFICATION-REPORT.md); the transcript is user-provided and was not independently captured by the assistant. The JAR used for scripted checks was built by `mvn clean verify` and launched with `java -jar target\course-registration-waitlist-1.0-SNAPSHOT.jar`.

| Scenario | Exact input | Expected behavior | Actual observed output excerpt | Result / evidence |
|---|---|---|---|---|
| CLI-A — Startup, HELP, VIEW, EXIT | `HELP`, `VIEW`, `EXIT` | Print commands, empty offering state (capacity 2, 2 seats), then exit. | `Offering capacity: 2`; `Enrolled learners (0):`; `Waitlisted learners (FIFO):`; `Available seats: 2`; `Goodbye.` | **PASS**; scripted process output captured during verification |
| CLI-B — Enroll then waitlist | `ENROLL eligible-1`; `ENROLL eligible-2`; `ENROLL eligible-3`; `VIEW` | First two enroll, third waits; queue is FIFO and seats are exhausted. | `ENROLLED: Enrollment confirmed.` twice; `WAITLISTED: Added to the FIFO waitlist.`; `0: eligible-3`; `Available seats: 0` | **PASS** |
| CLI-C — Duplicate outcomes | After CLI-B in same process: `ENROLL eligible-1`; `ENROLL eligible-3` | Existing enrollment and waitlist entry return clear duplicate results without mutation. | `DUPLICATE: Learner is already enrolled.`; `DUPLICATE: Learner is already on the waitlist.` | **PASS** |
| CLI-D — Drop/promotion/repeated drop | After CLI-B/C: `DROP eligible-1`; `VIEW`; `DROP eligible-1` | Drop triggers promotion of eligible-3; no promotion detail is invented in the result; subsequent drop is not enrolled. | `DROPPED: Enrollment removed.`; enrolled list contains eligible-2 and eligible-3; queue `(none)`; `NOT_ENROLLED: No active enrollment to drop.` | **PASS** |
| CLI-E — Validation, malformed commands, blank, EOF | Separate process: blank line; `UNKNOWN`; `ENROLL`; `DROP`; `ENROLL eligible-4 extra`; `DROP eligible-4 extra`; then close stdin | Invalid/unknown/malformed inputs produce clear messages and continue; EOF exits cleanly. | `Please enter a command...`; `Unknown command...`; `INVALID_REQUEST: A valid learner ID is required.`; `INVALID_REQUEST: Use ENROLL <learner-id>.`; `INVALID_REQUEST: Use DROP <learner-id>.`; `End of input. Exiting.` | **PASS** for these exact inputs |
| CLI-F — Human interactive run | Start JAR in a terminal and manually type commands | Verify actual keyboard interaction and console behavior | User reports running the JAR interactively. The transcript shows HELP, missing arguments, VIEW, enrollment, waitlisting, duplicates, drop/promotion, repeated drop, ineligible input, unknown command, and EXIT. See [MANUAL-VERIFICATION-REPORT.md](./MANUAL-VERIFICATION-REPORT.md). | **PASS (user-provided evidence)** |

For a repeatable manual interactive check in Windows PowerShell:

```powershell
mvn package
java -jar target\course-registration-waitlist-1.0-SNAPSHOT.jar
```

At the running prompt, enter `HELP`, `ENROLL eligible-1`, `ENROLL eligible-2`, `ENROLL eligible-3`, `VIEW`, `ENROLL eligible-1`, `ENROLL eligible-3`, `DROP eligible-1`, `VIEW`, `DROP eligible-1`, then `EXIT`. This resets no state automatically; start a fresh process for a clean offering. Report the observed results separately if this pending check is performed.

The CLI accepts learner IDs as whitespace-delimited tokens. A missing learner argument is verified as `INVALID_REQUEST`; exact identifier syntax beyond the domain's non-null/non-blank validation is not specified by the assignment.

## 8. Known limitations, risks, and unresolved findings

1. **AC-12 is only partially demonstrated.** [AI-USAGE-REPORT.md](./AI-USAGE-REPORT.md) records accessible current prompts, checkpoint summaries, artifacts, and verification. Raw historic prompts/responses and suggestion-by-suggestion dispositions were not found and must not be reconstructed or invented.
2. **Manual CLI evidence is user-provided.** The transcript in [MANUAL-VERIFICATION-REPORT.md](./MANUAL-VERIFICATION-REPORT.md) is treated as reported interactive evidence, not as output independently captured by the assistant. A repeatable command sequence remains available in section 7.
3. **Prerequisite fixture is illustrative.** Eligibility is the documented in-memory ID set, not an authoritative academic prerequisite rule or learner source.
4. **In-memory persistence is ephemeral and single-process.** State resets on restart. Concurrency tests exercise this synchronized repository only; they do not prove multi-process coordination, crash recovery, or database behavior.
5. **Defensive code branches remain uncovered.** These are the unreachable-with-concrete-repository stale enrollment removal path, stale waitlist removal guard, and the compiler-generated default branch for an unrecognized enum value. Main business, domain, and CLI source lines are covered; coverage thresholds pass.
6. **Test-plan documentation was not changed.** Existing test-plan IDs were used; CLI-only tests are additional interface tests. No missing requirement behavior test was identified.

No implementation-blocking defect or requirement contradiction was found. The remaining approval/readiness issue is completing AC-12 from genuine historic interaction evidence, if it is available.

## 9. Commands and evidence paths

```powershell
mvn clean verify
mvn -Dtest=RegistrationCliTest test
mvn -Dtest=EnrollmentServiceTest test
mvn package
java -jar target\course-registration-waitlist-1.0-SNAPSHOT.jar
```

Evidence artifacts:

- `target\surefire-reports\TEST-*.xml` — per-suite counters and individual test cases.
- `target\surefire-reports\*.txt` — Maven Surefire summaries.
- `target\site\jacoco\index.html` — readable coverage report.
- `target\site\jacoco\jacoco.xml` — machine-readable coverage counters and source-line details.
- `target\course-registration-waitlist-1.0-SNAPSHOT.jar` — executable CLI artifact used for scripted checks.

Artifacts under `target\` are generated and are not source deliverables; regenerate them with the commands above.

## 10. Final verification after documentation changes

The verification previously recorded below followed the verification-report and README updates at that time. For this AI usage report, an additional fresh `mvn clean verify` was executed on 2026-11-03 at 09:52 (+05:30); it cleaned and regenerated the target reports before the AI report/README/plan/verification-status documentation edits in the current task. Those later changes are documentation-only:

- **Tests:** 64 run; 64 passed; 0 failed; 0 errors; 0 skipped.
- **JaCoCo:** all configured checks passed; generated HTML/XML report under `target\site\jacoco\`.
- **Build:** `BUILD SUCCESS`; runnable JAR built.
- **Executable check:** the JAR was launched with scripted input and completed HELP, enrollment, VIEW, and EXIT successfully.
- **Readiness:** **CONDITIONAL GO** for code review and submission after AC-12's genuine AI-use record is provided. Manual CLI verification is recorded as user-provided evidence in `docs\MANUAL-VERIFICATION-REPORT.md`; the Maven and coverage figures in this report come from the assistant's own final verification run.
