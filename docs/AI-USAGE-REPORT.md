# AI Usage Report — Course Registration and Waitlist Management

## Executive Summary

AI was used as an engineering assistant during this project's requirements, design, implementation, testing, verification, and documentation work. The current interaction is running through Copilot SDK in VS Code. The accessible session checkpoints and project artifacts describe substantial earlier work, but the original historic Copilot prompts and responses are not available in the workspace or retrieved session history. Accordingly, this report distinguishes direct evidence, checkpoint summaries, and inference from artifacts; it does not invent historic prompt text, AI recommendations, dates, or human decisions.

A fresh `mvn clean verify` was executed on 2026-11-03 at 09:52 (+05:30). It passed **64 tests** with no failures, errors, or skips. The regenerated JaCoCo report shows **99.42% line** and **96.97% branch** coverage overall; the combined domain/application business code has **98.60% line** and **95.24% branch** coverage. These are execution results, not evidence that a specific AI suggestion was correct.

**AC-12 assessment: PARTIAL.** AI-assisted engineering and resulting artifacts are evidenced, and this report records the prompts and evidence that are actually accessible. AC-12 is not fully demonstrated because most historical prompts/responses and suggestion-by-suggestion human dispositions cannot be recovered. The report must not be interpreted as a reconstruction of those missing records.

## Purpose and Scope

This report documents AI assistance for the one-offering registration prototype: requirements analysis, architecture and planning, Java/Maven implementation, tests, verification, and follow-up documentation. It also provides an AC-12 evidence matrix.

Reviewed project inputs include [the requirements](../requirement.md), [implementation plan](../implementation-plan.md), [test plan](../test-plan.md), [README](../README.md), the five architecture documents in [architecture/](../architecture/), [verification report](./VERIFICATION-REPORT.md), [manual verification report](./MANUAL-VERIFICATION-REPORT.md), [future improvements](./FUTURE-IMPROVEMENTS.md), Java source and JUnit tests under [src/](../src/), and [pom.xml](../pom.xml). No prior AI usage report was present when this review began.

The report does not change the approved functional scope, requirements, or ADR decisions. It does not assert that every historical implementation choice originated with AI or that any individual performed a review unless the available evidence supports it.

## AI Tools Used

| Tool | Evidence and limits |
|---|---|
| Copilot assistant through Copilot SDK in VS Code | The current workspace assistant is identified as Copilot SDK in VS Code. Session-state checkpoint files and the supplied conversation summaries document earlier project phases, but they are summaries rather than raw prompt/response transcripts. |
| Other AI tools | No evidence was found that ChatGPT or another AI tool was used. This is not a claim that no other tool was used; it is a limit of the available records. |
| Copilot conversation history | The current task conversation and its supplied summaries are accessible. The local and cloud session-history queries returned no matching project sessions, and no raw historical chat export or saved prompt file was found. Historic prompt wording and model/tool-call details are therefore unavailable. |

## AI-Assisted Development Timeline

“Confirmed” below means that a user request, accessible current-session instruction, checkpoint summary, or resulting artifact supports the activity. It does not mean the exact historic AI response or a user's disposition of that response is available.

| Development phase | Evidence-supported account | Evidence level |
|---|---|---|
| Requirement analysis and clarification | A checkpoint summary records a request to derive a structured `requirement.md` from the assignment README, preserve explicit requirements, and separate assumptions and ambiguities. The resulting requirements document contains FR/BR/AC IDs and open questions. | Checkpoint summary and resulting document; exact prompt and response unavailable. |
| Architecture and C4 diagrams | Checkpoints record work on C4 context, container, and component views. The documents show the one-offering boundary and selected design policies; a later checkpoint records user-selected final policies. | Checkpoint summaries and architecture artifacts; exact prompt/response pair and suggestion dispositions unavailable. |
| Low-level design and ADRs | Checkpoints record LLD and nine ADRs, followed by alignment around FIFO, eligibility revalidation, skip-and-retain, existing-waiter priority, and transaction boundaries. | Resulting [LLD](../architecture/lld.md), [ADRs](../architecture/architecture-decisions.md), and checkpoint summary; raw AI advice/dispositions unavailable. |
| Implementation and test planning | Checkpoints record work on an incremental implementation plan and test plan, with traceability and concurrency/failure scenarios. | [Implementation plan](../implementation-plan.md), [test plan](../test-plan.md), and checkpoint summaries; exact historic interaction unavailable. |
| Maven setup and domain modeling | Checkpoints record Java 21/Maven/JUnit setup, domain types, a synchronized in-memory repository, and JaCoCo thresholds. `pom.xml`, source, and tests are present. | Artifacts and checkpoint summaries; specific AI recommendations and human disposition unavailable. |
| Repository transactions, enrollment, and prerequisites | Checkpoints record copy-on-write in-memory transactions, enrollment, duplicate checks, eligibility validation, and waitlist allocation. | Source/tests and checkpoint summaries; prompt-response transcript unavailable. |
| Drop, FIFO promotion, concurrency, and rollback | Checkpoints record the drop workflow, promotion policy, failure rollback, and focused concurrency cases. | Source/tests, test-plan mapping, verification evidence, and checkpoint summaries; historic prompts and response summaries unavailable. |
| CLI integration | The supplied historical user request explicitly scoped “Step 5: CLI Integration” and requested service delegation, text commands, standard Java, and JUnit coverage. CLI source and tests implement the described boundary. | User prompt excerpt and source/tests accessible in supplied conversation; the historic AI reply and disposition of individual suggestions are unavailable. |
| JUnit and JaCoCo verification | The verification report and fresh Maven run provide executable evidence for the suite and coverage. The project includes focused domain, application, repository, and CLI tests. | Fresh build and regenerated XML reports; these verify the code, not historic prompt provenance. |
| Final verification and manual testing | A historical user request specified final verification/reporting; later user-provided terminal output is preserved separately and explicitly attributed. | User requests, [verification report](./VERIFICATION-REPORT.md), [manual report](./MANUAL-VERIFICATION-REPORT.md), and fresh build; user-provided console evidence is not independently captured here. |
| Future NFR planning | A user request explicitly asked for proposed production NFRs, measurable illustrative targets, trade-offs, priorities, and a phased roadmap. | User prompt and [future improvements](./FUTURE-IMPROVEMENTS.md) are accessible; the proposed targets are not business-approved. |
| README and documentation consistency review | A user request asked to review the Markdown/architecture documents and update README. The README and design-document status notes were updated, and local Markdown links were checked. | Current conversation and resulting files; no code change or test run was needed for that documentation-only work. |
| AI usage report | The current user prompt requests this evidence-based report and prohibits fabricated history. This report is the response to that prompt. | Exact prompt is accessible in the current conversation; user review/disposition of this report is pending. |

## Detailed AI Prompt and Interaction Log

The table uses **actual prompt excerpt** only for text available in the current conversation. “Prompt summary” means a project checkpoint or supplied conversation summary describes the request, but the original wording is not available. Historic AI recommendations are marked unavailable rather than reconstructed from the final code or design.

| Interaction ID | Development phase | Engineering objective | AI tool | Prompt | AI recommendation / response evidence | Human decision | Reason | Files affected or associated | Verification |
|---|---|---|---|---|---|---|---|---|---|
| AI-001 | Requirements | Convert assignment into structured, traceable requirements without implementing code. | Copilot-assisted project session; exact historic tool metadata unavailable. | **Prompt summary from checkpoint:** analyze assignment README and create root `requirement.md`, separating explicit requirements, assumptions, decisions, and ambiguity. Exact wording is unavailable. | The resulting specification contains FR/BR/AC IDs, assumptions, scope, deliverables, and unresolved questions. The original AI response is unavailable. | **Unverified** for suggestion-level disposition. | Artifact presence establishes an output, not whether each suggestion was accepted or changed. | [requirement.md](../requirement.md) | Cross-referenced later by the test and verification matrices; no direct prompt-to-edit record. |
| AI-002 | C4 architecture | Describe system context, container, and component boundaries for the scoped one-offering feature. | Copilot-assisted project session; exact historic tool metadata unavailable. | **Prompt summary from checkpoint:** create C4 Level 1, Level 2, and Level 3 architecture documents. Exact wording unavailable. | Final diagrams model one learner and one application, with internal eligibility, waitlist, promotion, and repository responsibilities. Original AI suggestions are unavailable. | **Unverified** for individual suggestions. | The diagrams are evidence of design artifacts, not proof of a specific suggestion's acceptance. | [c4-context.md](../architecture/c4-context.md), [c4-container.md](../architecture/c4-container.md), [c4-component.md](../architecture/c4-component.md) | Documents were reviewed for scope and linked to requirements/ADRs; historic review transcript unavailable. |
| AI-003 | LLD / ADRs | Compare design alternatives and capture implementation boundaries and selected policies. | Copilot-assisted project session; exact historic tool metadata unavailable. | **Prompt summary from checkpoint:** create an LLD and ADRs; a later request selected nine final policies and asked for consistency updates. Exact prompts unavailable. | Final artifacts record one application, in-memory fallback, FIFO, revalidation, skip-and-retain, existing-waiter priority, atomic state changes where supported, and unresolved eligibility data. Original AI recommendation text is unavailable. | **Unverified** as to which were AI suggestions; final policy selection is recorded as user-directed in a checkpoint summary. | Do not label a selected user decision as acceptance/rejection of an unknown AI proposal. | [lld.md](../architecture/lld.md), [architecture-decisions.md](../architecture/architecture-decisions.md) | Later source/tests and the verification report provide behavioral evidence for selected policies. |
| AI-004 | Planning | Create staged implementation and test plans with dependencies and traceability. | Copilot-assisted project session; exact historic tool metadata unavailable. | **Prompt summary from checkpoint:** prepare implementation and test plans for the assignment; exact prompt unavailable. | Plans contain phased implementation, test scenarios, invariants, AC/TP mapping, and verification instructions. Original response unavailable. | **Unverified** for suggestion dispositions. | The plan files do not preserve per-suggestion decisions. | [implementation-plan.md](../implementation-plan.md), [test-plan.md](../test-plan.md) | Actual implementation and test cases were compared with plan/IDs in the verification report. |
| AI-005 | Maven / domain / repository | Set up Java/Maven/JUnit, domain state, and an in-memory synchronized transaction boundary. | Copilot-assisted project session; exact historic tool metadata unavailable. | **Prompt summary from checkpoint:** implement project setup and domain/in-memory repository stages; exact prompts unavailable. | Source contains Java 21 records, a synchronized copy-on-write repository, snapshots, duplicate guards, and tests. The AI's specific recommendations and response are not preserved. | **Unverified** for AI suggestion disposition. | Source inspection cannot reveal who proposed each implementation detail. | [pom.xml](../pom.xml), [domain source](../src/main/java/com/example/coursewaitlist/domain/), [repository source](../src/main/java/com/example/coursewaitlist/persistence/) | Fresh `mvn clean verify` and JaCoCo output described below; this does not establish historical authorship. |
| AI-006 | Enrollment / prerequisite validation | Implement eligibility checks, duplicate outcomes, and existing-waiter priority before allocating a seat to a new requester. | Copilot-assisted project session; exact historic tool metadata unavailable. | **Prompt summary from checkpoint:** implement prerequisite validation and enrollment; exact wording unavailable. | `EnrollmentService` and `PromotionCoordinator` implement eligibility, duplicates, and waitlist allocation. No historic AI response is accessible. | **Unverified** for suggestion-level disposition. | The final implementation alone does not establish whether suggestions were accepted or modified. | [EnrollmentService.java](../src/main/java/com/example/coursewaitlist/application/EnrollmentService.java), [PromotionCoordinator.java](../src/main/java/com/example/coursewaitlist/application/PromotionCoordinator.java), [EnrollmentServiceTest.java](../src/test/java/com/example/coursewaitlist/application/EnrollmentServiceTest.java) | Fresh `mvn clean verify`; TP-01 through TP-07 and LLD-AC cases in service tests. |
| AI-007 | Drop / promotion / rollback / concurrency | Reuse promotion behavior for drops and preserve state when technical failures occur; verify concurrency invariants. | Copilot-assisted project session; exact historic tool metadata unavailable. | **Prompt summary from checkpoint:** implement the drop step and focused tests, including concurrency and rollback; exact wording unavailable. | Source runs drop, promotion, and enrollment within repository transactions. Historic AI response is unavailable. | **Unverified** for suggestion disposition. | The checkpoint records work/results, not a per-suggestion review record. | [EnrollmentService.java](../src/main/java/com/example/coursewaitlist/application/EnrollmentService.java), [InMemoryRegistrationRepository.java](../src/main/java/com/example/coursewaitlist/persistence/InMemoryRegistrationRepository.java), [EnrollmentServiceTest.java](../src/test/java/com/example/coursewaitlist/application/EnrollmentServiceTest.java), [InMemoryRegistrationRepositoryTest.java](../src/test/java/com/example/coursewaitlist/persistence/InMemoryRegistrationRepositoryTest.java) | Fresh tests include injected validator failure, concurrent enrollment/drop, repeated drop, and repository rollback tests. |
| AI-008 | CLI integration | Add only a simple CLI boundary that delegates to existing services. | Copilot-assisted project session; exact historic tool metadata unavailable. | **Actual prompt excerpt:** “Implement ONLY Step 5: CLI Integration.” The request also specified a text CLI, service delegation, constructor-injected streams where practical, and JUnit integration tests. | CLI source parses and displays outcomes while calling the existing enrollment service and repository snapshot. The AI response itself is not preserved. | **Unverified** for suggestion-level disposition; the bounded Step 5 scope was user-directed. | No raw response exists to classify accepted/modified/rejected suggestions. | [Main.java](../src/main/java/com/example/coursewaitlist/Main.java), [RegistrationCli.java](../src/main/java/com/example/coursewaitlist/cli/RegistrationCli.java), [DemoConfiguration.java](../src/main/java/com/example/coursewaitlist/cli/DemoConfiguration.java), [RegistrationCliTest.java](../src/test/java/com/example/coursewaitlist/cli/RegistrationCliTest.java), [pom.xml](../pom.xml) | Fresh full suite; 13 CLI tests passed; the user-provided interactive transcript is separately attributed in the manual report. |
| AI-009 | Final verification | Inspect actual build, test, coverage, requirement traceability, invariants, and CLI behavior without fabricating evidence. | Copilot-assisted project session; exact historic tool metadata unavailable. | **Actual prompt excerpt:** “Implement ONLY Step 6: Final Verification and Reporting.” The request required actual Maven/JaCoCo/Surefire evidence and a verification report. | [VERIFICATION-REPORT.md](./VERIFICATION-REPORT.md) records requirement traceability, coverage, risks, and verification outcomes. Historic AI response unavailable. | **Unverified** for historic suggestion dispositions. | Report content and saved artifacts show work product but not the prompt-response decision trail. | [VERIFICATION-REPORT.md](./VERIFICATION-REPORT.md), [README.md](../README.md) | Fresh `mvn clean verify` executed for this AI usage report; details and evidence paths below. |
| AI-010 | Manual evidence recording | Preserve user-provided console and focused Maven output as attributed evidence. | Copilot assistant in the current project history; raw historical tool metadata unavailable. | **Prompt summary:** user asked to add supplied `mvn clean verify`, CLI, EnrollmentServiceTest, and RegistrationCliTest evidence to the manual report. | The manual report records supplied transcripts and labels them user-provided rather than independently captured. | **Accepted as a documentation task** (the user explicitly requested inclusion); not an AI-generated business-rule decision. | Preserve source and provenance; do not infer missing output or count focused reruns as new tests. | [MANUAL-VERIFICATION-REPORT.md](./MANUAL-VERIFICATION-REPORT.md), [VERIFICATION-REPORT.md](./VERIFICATION-REPORT.md) | Transcript content cross-checked against the user-provided output in the accessible conversation. |
| AI-011 | Future NFR planning | Propose future production NFRs, measurable examples, trade-offs, priorities, and phases without changing current scope. | Copilot assistant; exact historic tool metadata unavailable. | **Actual prompt excerpt:** “Create docs/FUTURE-IMPROVEMENTS.md.” The request explicitly required proposed targets and separation of NFRs from new functional capabilities. | The document labels all numerical targets proposed, subject to load testing and approval, and separates new capabilities. Historic per-target AI response is unavailable. | **Unverified** for approval of any proposed target; user asked for the document, not adoption of its targets. | No business approval is evidenced; the document explicitly states proposals only. | [FUTURE-IMPROVEMENTS.md](./FUTURE-IMPROVEMENTS.md) | Documentation reviewed for scope framing; no production load test or stakeholder approval is claimed. |
| AI-012 | README and documentation consistency | Review project Markdown and architecture documents, then make README the navigation and execution entry point. | Copilot assistant in this current conversation. | **Actual prompt excerpt:** “validate and review all the docs and *.md files in architecture folder and update the README.md file”. | README was replaced with project purpose, setup, run commands, demo, limitations, and document links; stale initial-baseline wording was clarified in selected docs. | **Accepted as requested documentation work**; the user directed the scope. | This is not evidence that historical code suggestions were accepted or rejected. | [README.md](../README.md), architecture documents, [implementation-plan.md](../implementation-plan.md), [test-plan.md](../test-plan.md) | Local Markdown links were checked across 13 Markdown files; no application code changed. |
| AI-013 | AI usage record / current report | Create a candid AC-12 report from accessible evidence and identify missing history. | Copilot SDK in VS Code (current task). | **Actual prompt:** the current request beginning “Generate a comprehensive, professional, evidence-based AI Usage Report…” with explicit no-fabrication constraints. | This report and the documentation updates listed below are the response. Historic activities are labeled by evidence type. | **Pending** user review of this report; no acceptance is presumed. | The current user prompt requires an auditable report and forbids invented history. | [AI-USAGE-REPORT.md](./AI-USAGE-REPORT.md), [README.md](../README.md), [implementation-plan.md](../implementation-plan.md), [VERIFICATION-REPORT.md](./VERIFICATION-REPORT.md) | Current fresh Maven build verifies project code; report link/wording and Markdown links are checked separately. |

### Accepted, Modified, and Rejected AI Suggestions

The available records do **not** contain a trustworthy list of historic AI suggestions with corresponding human dispositions. Consequently, this report does not claim that a particular historic AI suggestion was accepted, modified, or rejected. The selected ADR policies are recorded in the checkpoints as user-selected decisions, but the missing prompt/response chain prevents classifying them as dispositions of identified AI suggestions.

The following are supported statements rather than reconstructed suggestion decisions:

| Item | Evidence-supported status |
|---|---|
| Final fixed business/design policies in ADR-01 through ADR-09 | Selected policies are documented and later tested; whether each originated as an AI suggestion is **unverified**. |
| Proposed numerical production SLOs in FUT-NFR-01 through FUT-NFR-12 | **Not approved**; explicitly illustrative proposals pending load testing and business approval. |
| Current report structure and evidence treatment | **User-directed** by the current prompt; user's final acceptance of this report is pending. |
| Historic AI recommendations not visible in the conversation/checkpoints | **Unverified**; no status assigned. |

## Engineering Decision Case Studies

These case studies distinguish the implemented behavior and its evidence from any unavailable historic AI advice. A design visible in an ADR or source file is not, by itself, proof that AI proposed it or that a human accepted a specific AI suggestion.

### A. In-memory transactional repository

- **Problem:** Keep offering capacity, enrollment, active-registration uniqueness, and waitlist state consistent in the permitted single-process, in-memory implementation.
- **AI prompt or available summary:** Checkpoint summary records implementation of synchronized in-memory state and transaction behavior; original prompt and response are unavailable.
- **AI-generated suggestion:** Not recoverable from the evidence.
- **Human review and decision:** The copy-on-write behavior is implemented and described in project artifacts. A specific human acceptance/rejection of an AI proposal is unverified.
- **Final implementation:** [`InMemoryRegistrationRepository`](../src/main/java/com/example/coursewaitlist/persistence/InMemoryRegistrationRepository.java) synchronizes `snapshot` and `transact`, copies state into a working set, invokes the callback, then publishes `workingState.freeze()` only on normal return. A callback exception leaves the committed `state` reference unchanged; `finally` closes the transaction and releases the active flag. The transaction view is thread-bound and cannot be used after completion. Snapshots and returned collections are immutable copies.
- **Verification evidence:** The fresh 64-test suite includes repository rollback, immutable snapshots, guarded duplicate writes, and concurrent write tests. See [repository tests](../src/test/java/com/example/coursewaitlist/persistence/InMemoryRegistrationRepositoryTest.java) and `target/surefire-reports/TEST-com.example.coursewaitlist.persistence.InMemoryRegistrationRepositoryTest.xml`.
- **Trade-offs and lessons:** This gives atomic visibility and rollback within one repository instance/JVM, but state is lost on restart and independent processes cannot coordinate. It is not a durable or distributed transaction system.

### B. FIFO waitlist promotion

- **Problem:** Allocate released or open seats to the earliest currently eligible waiting learner without discarding ineligible learners.
- **AI prompt or available summary:** Checkpoint and ADR history records FIFO, revalidation, and skip-and-retain policy work; exact prompt and AI recommendation unavailable.
- **AI-generated suggestion:** Not recoverable.
- **Human review and decision:** The finalized policy is documented as selected in ADR-06 and in the supplied checkpoint summary; its status as a response to an identifiable AI proposal is unverified.
- **Final implementation:** [`WaitlistManager`](../src/main/java/com/example/coursewaitlist/application/WaitlistManager.java) traverses position-ordered immutable waitlist entries. [`PromotionCoordinator`](../src/main/java/com/example/coursewaitlist/application/PromotionCoordinator.java) advances the scan cursor in FIFO order, rechecks eligibility, retains ineligible entries in place, and removes/enrolls the first eligible candidate while a seat remains. `EnrollmentService.enroll` invokes this allocation before giving an available seat to the new eligible requester.
- **Verification evidence:** Service tests cover FIFO, revalidation, retained positions, later eligibility changes, existing-waiter priority, and promotion of multiple eligible waiters. Fresh suite passed.
- **Trade-offs and lessons:** Rechecking candidates and retaining their positions improves fairness to eligible learners and keeps temporarily ineligible entries, at the cost of scanning the queue on allocation. This fixed policy does not provide configurable waitlist behavior.

### C. Concurrent enrollment

- **Problem:** Prevent requests from overbooking seats or creating duplicate active registrations when submitted concurrently.
- **AI prompt or available summary:** Checkpoints and test plan record practical concurrency testing; original prompt and response unavailable.
- **AI-generated suggestion:** Not recoverable.
- **Human review and decision:** The selected implementation uses repository synchronization; whether this exactly reflects an AI recommendation is unverified.
- **Final implementation:** `synchronized transact` serializes operations on a repository instance. The active-registration index is copied with the staged state; `saveEnrollment` checks active membership and capacity before updating enrollment plus index. `EnrollmentService.enroll` performs duplicate lookup, eligibility validation, priority allocation, and final enrollment/waitlist decision inside one transaction.
- **Verification evidence:** JUnit tests use `CountDownLatch`, `ExecutorService`, and bounded futures for distinct concurrent enrollment, duplicate active registration, enrollment/drop races, concurrent drops, and repeated drops. Assertions inspect final capacity and uniqueness invariants.
- **Trade-offs and lessons:** This establishes in-process coordination for a shared repository instance, not multi-JVM or multi-host correctness. A production shared store would require persistence-level atomicity and uniqueness protection.

### D. Drop and automatic promotion

- **Problem:** Release a seat on a valid drop and promote the earliest eligible waiter without publishing partial state if a later technical step fails.
- **AI prompt or available summary:** Checkpoint records Step 4 drop/promotion, rollback, and concurrency work; exact prompt/AI response unavailable.
- **AI-generated suggestion:** Not recoverable.
- **Human review and decision:** Specific historical suggestion dispositions are unavailable.
- **Final implementation:** `EnrollmentService.drop` looks up and removes an enrolled record inside one `transact` callback and calls the same promotion coordinator before returning `DROPPED`. If no eligible waiter is found, the drop commits and the seat remains free. If eligibility or mutation throws, the staged copy is not published.
- **Verification evidence:** `EnrollmentServiceTest` injects a later eligibility failure, compares the complete snapshot to pre-operation state, retries, and checks committed promotion. Other tests cover no eligible waiter, FIFO promotion, invalid/repeated drop, and concurrent drops.
- **Trade-offs and lessons:** One in-memory transaction provides a simple atomic workflow. The service does not expose a promoted-learner detail in its CLI result, and cross-store atomicity is not implemented.

### E. CLI integration

- **Problem:** Provide a runnable interface for the assignment without expanding into a frontend or network service, and avoid putting business rules into presentation code.
- **AI prompt or available summary:** The user-supplied Step 5 request explicitly calls for “CLI Integration,” standard Java, a text command loop, service reuse, and testable streams.
- **AI-generated suggestion:** The available source shows a small CLI architecture; historic assistant response text is not available, so no additional suggestion is attributed.
- **Human review and decision:** The user explicitly scoped the implementation to CLI integration and excluded REST/GUI additions. Individual historic response dispositions are unverified.
- **Final implementation:** [`RegistrationCli`](../src/main/java/com/example/coursewaitlist/cli/RegistrationCli.java) handles parsing, output, and snapshot display; [`DemoConfiguration`](../src/main/java/com/example/coursewaitlist/cli/DemoConfiguration.java) assembles service/repository/eligibility fixture; [`Main`](../src/main/java/com/example/coursewaitlist/Main.java) starts the app. It supports `ENROLL`, `DROP`, `VIEW`, `HELP`, `EXIT`, malformed input, and EOF handling.
- **Verification evidence:** Thirteen CLI JUnit tests use in-memory readers/writers; a runnable JAR transcript is also recorded as user-provided in the [manual report](./MANUAL-VERIFICATION-REPORT.md).
- **Trade-offs and lessons:** The CLI is adequate for the bounded deliverable and keeps the implementation dependency-light. It has no network protocol, authentication, durable state, or production operational interface.

### F. Testing and coverage

- **Problem:** Verify business rules, state invariants, failure rollback, concurrency, and the CLI, while keeping the suite deterministic and measurable.
- **AI prompt or available summary:** Checkpoints record JUnit and JaCoCo work; no exact historic test-generation prompt or AI response is available.
- **AI-generated suggestion:** Not recoverable. Test implementation and report evidence do not identify which test cases were AI-proposed.
- **Human review and decision:** A per-test-case human accept/modify/reject record is unavailable.
- **Final implementation:** JUnit 5 test classes cover domain validation, eligibility fixture, service workflows, repository transactions/concurrency, CLI integration, and Java 21 setup. Maven configures JaCoCo 0.8.13 with 90% line and 85% branch bundle minimums.
- **Verification evidence:** A fresh `mvn clean verify` passed all 64 tests and JaCoCo checks. XML and HTML reports were regenerated in `target/surefire-reports/` and `target/site/jacoco/`.
- **Trade-offs and lessons:** Coverage quantifies execution but does not prove correctness or authorship. In-process concurrency tests do not establish distributed guarantees. The current checks include a small number of intentionally uncovered defensive/generated branches.

## Human Review and Independent Verification

The available evidence supports the following forms of review and verification:

- **Requirements and scope:** The implementation and reports trace to requirement/test-plan IDs. The finalized decisions distinguish assignment requirements from selected policies.
- **Architecture:** C4, LLD, and ADR documents state boundaries, alternatives, constraints, and unresolved areas. A later source review updated stale “not yet implemented” framing where appropriate.
- **Code and behavior:** The Java source can be inspected directly; the verification report maps selected invariants to source and test methods. No historical line-by-line human code-review record is available.
- **JUnit and build:** The fresh `mvn clean verify` command executed successfully in this task. Exact results are listed below.
- **JaCoCo:** The fresh XML report was parsed after that run. Percentages are computed from missed/covered counters; no exclusions were added for this report.
- **Concurrency and rollback:** Fresh suite runs include tests for concurrent operations and injected validator failures. They validate this in-memory repository/JVM only.
- **CLI manual evidence:** The terminal output in the manual report was supplied by the user. It is explicitly not represented as a console run independently captured by the assistant.
- **Trade-offs and scope:** Requirements, ADRs, and future NFRs keep multi-course, REST, GUI, external identity, persistence, deployment, and notification capability out of the current application. Future NFR targets are proposals, not accepted production commitments.

AI assistance should be treated as a drafting, implementation, and test-support tool; final behavior is supported here by source inspection and fresh automated execution. The absent historic transcript means this report cannot substantiate the exact review process that occurred during earlier phases.

## Automated Testing and Coverage Evidence

### Fresh execution

On **2026-11-03 09:52 (+05:30)**, the following command was run from the project root:

```powershell
mvn clean verify
```

Maven recompiled 23 main source files and 6 test source files, generated the runnable JAR, regenerated Surefire/JaCoCo reports, and reported `BUILD SUCCESS`. The fresh Surefire totals were:

| Test suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `DemoPrerequisiteValidatorTest` | 3 | 0 | 0 | 0 |
| `EnrollmentServiceTest` | 27 | 0 | 0 | 0 |
| `RegistrationCliTest` | 13 | 0 | 0 | 0 |
| `DomainModelsTest` | 7 | 0 | 0 | 0 |
| `InMemoryRegistrationRepositoryTest` | 13 | 0 | 0 | 0 |
| `ProjectSetupTest` | 1 | 0 | 0 | 0 |
| **Total unique test cases** | **64** | **0** | **0** | **0** |

### Fresh JaCoCo counters

The following figures come from `target/site/jacoco/jacoco.xml` generated by the fresh build, not from assumptions or a prior manual transcript. The business/domain total combines the `application` and `domain` packages.

| Scope | Lines covered / total | Line coverage | Branches covered / total | Branch coverage |
|---|---:|---:|---:|---:|
| Overall bundle | 342 / 344 | 99.42% | 96 / 99 | 96.97% |
| Domain package | 58 / 58 | 100.00% | 12 / 12 | 100.00% |
| Application package | 83 / 85 | 97.65% | 28 / 30 | 93.33% |
| **Business/domain combined** | **141 / 143** | **98.60%** | **40 / 42** | **95.24%** |
| Persistence package | 110 / 110 | 100.00% | 16 / 16 | 100.00% |
| CLI package | 89 / 89 | 100.00% | 40 / 41 | 97.56% |
| Main entry point | 2 / 2 | 100.00% | 0 / 0 | N/A |

These exceed the configured Maven gates of 90% line and 85% branch coverage at bundle level. JaCoCo reports two missed lines and three missed branches overall. The previously documented defensive stale-removal paths and generated CLI enum fallback are noted in the [verification report](./VERIFICATION-REPORT.md); no class was excluded.

### Saved versus fresh evidence

Before this run, the workspace contained Surefire XML/text reports and JaCoCo XML/HTML artifacts with the same 64-test and coverage counters. Those were saved build artifacts and could have reflected an earlier test selection. The fresh `mvn clean verify` ran `clean`, deleted the old `target` tree, and recreated all relevant test and coverage reports. The figures above are therefore verified against the regenerated files:

- `target/surefire-reports/TEST-*.xml`
- `target/surefire-reports/*.txt`
- `target/site/jacoco/jacoco.xml`
- `target/site/jacoco/index.html`
- `target/course-registration-waitlist-1.0-SNAPSHOT.jar`

The user's prompt-supplied `mvn clean verify` excerpt and focused Maven runs in [MANUAL-VERIFICATION-REPORT.md](./MANUAL-VERIFICATION-REPORT.md) remain separately attributed user evidence. Focused test reruns are not counted as additional unique cases in the fresh 64-test total.

## AI Limitations, Risks, and Mitigations

| Limitation or risk | Mitigation / reporting treatment |
|---|---|
| Raw historic Copilot prompts and responses are unavailable. | Identify checkpoint summaries as summaries; do not invent quotations or assistant recommendations. Request raw exports if complete retrospective traceability is required. |
| Historic model/tool version and exact per-phase authorship are not established. | Identify only the current Copilot SDK environment; avoid attributing individual source lines or decisions to AI. |
| Per-suggestion human dispositions are missing. | Mark them Unverified; state explicitly that final ADR decisions do not prove acceptance/rejection of a known AI suggestion. |
| Git cannot reconstruct development history. | The accessible log contains one initial commit (`d7c3d18`, `2026-11-02`) and the worktree is dirty; it does not establish later file-level authorship. |
| User-provided interactive CLI evidence could be mistaken for an independently run check. | Keep its user-provided provenance explicit in the manual report and verification report. |
| Coverage could be mistaken for correctness or provenance. | Report actual test methods, test results, and JaCoCo counters separately; do not treat percentage as proof that AI output is correct. |
| Illustrative production targets could be treated as approved SLOs. | Keep them explicitly proposed and approval/load-test dependent in FUTURE-IMPROVEMENTS. |
| The eligible learner set is a demo fixture. | State that it is not an authoritative prerequisite system or production identity source. |

## Lessons Learned

1. Keep assignment requirements, architecture choices, implementation decisions, and future proposals separately labeled.
2. Record prompts, relevant response summaries, disposition, affected files, and verification evidence when each AI-assisted task occurs; reconstructing that record later is unreliable.
3. Use tests that assert committed state and invariants, not only returned outcomes.
4. Treat concurrency and rollback as explicit verification concerns even in a small in-memory prototype.
5. Attribute manual evidence to its actual source, and distinguish that evidence from a fresh local execution.
6. Use JaCoCo as a diagnostic and threshold tool, not as a substitute for review or test quality.
7. Preserve a clear list of unresolved production needs instead of silently turning illustrative NFRs into product commitments.

## AC-12 Compliance Matrix

AC-12 in [requirement.md](../requirement.md) requires AI use for at least one engineering activity and an associated record of prompts, relevant AI responses/summaries, accepted/edited/rejected suggestions, affected files/decisions, and verification evidence.

| AC-12 requirement or evidence item | Supporting report section | Actual evidence source | Status | Missing information or action |
|---|---|---|---|---|
| AI used for at least one engineering activity | AI Tools Used; timeline; interaction log | Current Copilot-assisted report task; supplied checkpoints and resulting project artifacts | **PARTIAL** | The current report task is direct evidence of AI-assisted engineering, but historical tool/session attribution for earlier implementation phases is incomplete. |
| Prompts used are recorded | Detailed AI Prompt and Interaction Log | Exact current prompts and select prompt excerpts; prior checkpoint summaries | **PARTIAL** | Supply raw historic prompt exports or mark the checkpoint summaries as the only available record; exact earlier prompts remain unavailable. |
| Relevant AI response or summary is recorded | Interaction log; case studies | Artifacts and checkpoints summarize outcomes, but raw response summaries per prompt are absent | **PARTIAL** | Provide contemporaneous response summaries or original history. Do not infer the recommendation from final code. |
| Each suggestion disposition is recorded | Accepted, Modified, and Rejected AI Suggestions | No historic suggestion/disposition ledger found | **PENDING** | A developer with access to the actual prompt history must classify identifiable suggestions. Unknown suggestions must not be assigned a disposition. |
| Affected files or decisions are identified | Interaction log; case studies | Source/documents in workspace and checkpoint summaries | **PARTIAL** | Phase-level associations are available, but exact prompt-to-file mapping is not preserved. |
| AI-assisted work is independently verified | Human Review and Independent Verification; Automated Testing and Coverage Evidence | Fresh `mvn clean verify`, regenerated Surefire/JaCoCo reports, source/test review; user-supplied manual transcript separately attributed | **PASS for current verification evidence** | These results verify current behavior, but cannot prove what specific historic AI output was independently reviewed. |
| **AC-12 overall** | This report and referenced evidence | Available current prompt, checkpoints, project files, fresh build; no raw historic prompt/response log | **PARTIAL — criterion not yet fully demonstrated** | Add genuine historical interaction records and actual human dispositions if available. Until then retain conditional readiness; do not claim full AC-12 compliance. |

## Evidence References

- [Assignment-derived requirements, including AC-12](../requirement.md)
- [Implementation plan and planning-time baseline](../implementation-plan.md)
- [Test plan and TP identifiers](../test-plan.md)
- [README and run commands](../README.md)
- [C4 context](../architecture/c4-context.md), [C4 container](../architecture/c4-container.md), and [C4 components](../architecture/c4-component.md)
- [LLD](../architecture/lld.md) and [finalized ADRs](../architecture/architecture-decisions.md)
- [Verification report](./VERIFICATION-REPORT.md) and [manual verification report](./MANUAL-VERIFICATION-REPORT.md)
- [Future production NFR proposals](./FUTURE-IMPROVEMENTS.md)
- [Maven build configuration](../pom.xml)
- [Application source](../src/main/java/com/example/coursewaitlist/) and [JUnit tests](../src/test/java/com/example/coursewaitlist/)
- Fresh test artifacts under `target/surefire-reports/`; fresh JaCoCo artifacts under `target/site/jacoco/`.
- Accessible Git evidence: one initial commit, `d7c3d18bca58bb3320b01512b8f214a613e8a339`, dated 2026-11-02 19:27 +05:30, with subject “Initial commit.” The working tree has uncommitted changes. This commit contains only the initial README and provides no development interaction log.
- Accessible session evidence: three checkpoint summaries named for architecture finalization, enrollment implementation, and verification evidence. These are summaries, not raw conversation logs.

## Outstanding Gaps and Final Assessment

The report documents project artifacts, current conversation prompts, checkpoint summaries, user-supplied manual output, and a fresh verification run. It does not have access to raw historical Copilot conversations, saved prompt files, or a complete commit history. It therefore cannot reliably identify historic assistant recommendations or assign Accepted/Modified/Rejected labels to them.

**Final assessment: AC-12 PARTIAL; overall verification remains CONDITIONAL GO.** The current report is a useful evidence index, but it does not turn unavailable history into a complete historical AI-use record. If the original user can access exported Copilot prompts/responses, add them with accurate dispositions and file mappings, then update the matrix. No code change or new dependency was made for this report.
