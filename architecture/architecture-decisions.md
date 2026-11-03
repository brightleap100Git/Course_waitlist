# Architecture Decision Record — Course Registration and Waitlist Management

## 1. Purpose and status

This document evaluates implementation-level architecture and technical decisions against [the requirements](../requirement.md), [C4 context](./c4-context.md), [C4 container](./c4-container.md), [C4 component](./c4-component.md), and [LLD](./lld.md).

When these ADRs were prepared, the workspace contained assignment/design documents but no starter application source, declared stack, or persistence implementation. Their selected architectural policies are technology-neutral and record design choices made for the scoped feature, not a literal description of source structure. The project has since been implemented in Java 21 with a CLI and in-memory repository. See [the project README](../README.md) and [verification report](../docs/VERIFICATION-REPORT.md) for the implementation and verification evidence. Any supplied starter's technology, model, persistence, transaction/locking behavior, interfaces, and test conventions would take precedence.

### Decision status

- **Selected:** Chosen policy for this design, subject to compatibility with starter technology.
- **Assignment constraint:** Explicitly required or prohibited by the assignment.
- **Unresolved:** Requires starter details; no technology-specific choice is inferred.

## 2. Decision summary

| ID | Topic | Selected approach | Status |
|---|---|---|---|
| ADR-01 | Architecture style | One modular application; reuse the starter architecture or use the permitted small service/CLI fallback. | Selected |
| ADR-02 | State storage | Reuse starter persistence; use in-memory storage only if no starter exists. | Selected by assignment constraint |
| ADR-03 | Concurrent enrollment and seat allocation | Make capacity allocation and registration changes atomic using the starter's transaction/locking pattern or the simplest suitable in-memory synchronization. | Selected; mechanism remains technology-specific |
| ADR-04 | Duplicate enrollment prevention | Enforce one active registration per learner atomically, including concurrent requests. | Selected |
| ADR-05 | FIFO waitlist ordering | Use deterministic ordering and append eligible waitlisted learners at the tail. | Selected |
| ADR-06 | Promotion and seat-allocation priority | Revalidate eligibility, skip but retain ineligible candidates at their original positions, promote earliest eligible waitlisted learners before new requests receive available seats. | Selected |
| ADR-07 | Transaction boundaries | Make enrollment changes and drop-plus-promotion atomic where supported; preserve consistent state if the starter cannot span the full workflow. | Selected policy; mechanism remains technology-specific |
| ADR-08 | Error handling | Return clear outcomes for duplicates, repeated drops, and invalid requests; surface technical failures through starter conventions. | Selected |
| ADR-09 | Eligibility source and rule | Use the applicable prerequisite rule/data supplied by the starter; do not invent a rule or external integration. | Rule/data unresolved |

## 3. Decision records

### ADR-01 — Architecture style

**Problem.** The feature needs a request boundary, enrollment/drop behavior, prerequisite validation, waitlist/FIFO behavior, and persistence. The assignment is for one offering and expressly asks for a reliable service slice, not a full registration platform.

**Approaches considered**

1. **One application with cohesive internal responsibilities** (request boundary, use-case service, eligibility check, queue/promotion logic, persistence adapter).
2. **Separate deployable services** for enrollment, prerequisites, waitlist, and/or persistence.
3. **Single undifferentiated handler** containing request parsing, business logic, and state updates.

| Trade-off | One application, cohesive responsibilities | Separate deployable services | Undifferentiated handler |
|---|---|---|---|
| Consistency/concurrency | Related state changes can share one process and starter-supported transaction. | Cross-service consistency and coordination are harder and unnecessary for this scope. | Potentially straightforward initially, but state invariants and side effects become difficult to reason about. |
| Performance/scalability | Low communication overhead; adequate for one offering and unspecified load. | Independent scaling is possible but unsupported by assignment needs and adds network overhead. | Low overhead, but growth makes changes and testing harder. |
| Complexity/maintainability | Small boundaries clarify responsibilities without infrastructure proliferation. | Adds deployment, communication, failure, and operational concerns. | Fewer initial types but tightly coupled responsibilities and broader regression risk. |
| Testability | Business workflows and queue rules can be tested independently while preserving a small application. | Requires integration/contract testing between services. | Unit tests become coupled to the handler and state mutation details. |

**Selected approach.** Use one modular application with cohesive internal responsibilities, matching the C4 container and component diagrams. Reuse the starter's architecture and interaction shape; where there is no starter, use the permitted small service or CLI without requiring a web controller.

**Rejected alternatives.** Separate deployable services add communication, operational, and cross-service consistency costs without a requirement for them. An undifferentiated handler is rejected because it couples request handling, business decisions, and state updates.

**Rationale and trade-offs.** This fits the bounded one-offering slice, keeps enrollment and promotion close to shared state, and supports focused tests without unnecessary infrastructure. The cost is that logical concerns share one deployment boundary; independent service scaling is not a goal here.

**Assumptions and consequences.** Named components/classes are logical responsibilities, not a mandate to create a class for each role. Preserve a simpler suitable starter structure. This does not imply a frontend, API protocol, deployment design, or additional actors.

### ADR-02 — Registration state storage

**Problem.** Enrollment, capacity, and ordered waitlist state must remain consistent, but no database or persistence technology is specified.

**Approaches considered**

1. **Reuse starter-project persistence and its existing data model.**
2. **Use a small in-memory store** if no starter implementation exists.
3. **Introduce a new database or external persistence service.**

| Trade-off | Starter persistence / in-memory fallback | New database/service |
|---|---|---|
| Consistency | Can preserve the starter's established model and transaction behavior. In-memory state can maintain invariants within its process lifetime. | Durable transactions may be available, but require schema, configuration, migration, and consistency design not requested. |
| Concurrency | Depends on starter implementation; in-memory concurrency requires coordination appropriate to its execution model. | Can support concurrent transactions but introduces technology-specific choices and setup. |
| Performance/scalability | Adequate for the scoped feature; in-memory state is limited to process lifetime and a single process. | Potentially durable and multi-process, but not justified by stated scale or deployment requirements. |
| Complexity/testability | Reuses existing conventions; in-memory fallback is simple to exercise in focused tests. | More setup and integration tests; may make assignment review less portable. |

**Selected approach.** Use the starter's persistence and data model when present. If there is no starter implementation, use the permitted in-memory store. Do not introduce a new database unless the starter requires one.

**Rejected alternative.** A new database or external persistence service is not introduced because the assignment does not require one and the starter's needs are not known.

**Rationale and trade-offs.** This follows FR-10 and NFR-04 and avoids unsupported technology choices. In-memory state is simple and testable but loses state on process termination and does not provide multi-process persistence.

**Assumptions and consequences.** If durability, restart recovery, or multiple application instances are required, the starter must provide the relevant persistence or the scope must be reconsidered; this ADR does not authorize adding a database.

### ADR-03 — Concurrent enrollment and seat allocation

**Problem.** Concurrent requests must not claim the same remaining seat or produce state above capacity. The assignment only specifies concurrency handling conditionally: if the starter uses concurrent requests, preserve its transaction or locking pattern.

**Approaches considered**

1. **Use the starter's existing transaction/locking pattern around the capacity decision and state write.**
2. **Rely on a check-then-write sequence without synchronization.**
3. **Add a new lock/transaction framework or distributed coordination mechanism.**

| Trade-off | Existing starter pattern | Unsynchronized check-then-write | New coordination infrastructure |
|---|---|---|---|
| Consistency/concurrency | Can make seat allocation consistent within the starter's supported model. | Race condition can overbook or create duplicate active registrations. | Could coordinate requests, but complexity and correctness depend on technology and scope. |
| Performance | Uses the existing serialization/transaction cost; no new hop. | Fast in uncontended cases but incorrect under races. | Adds coordination overhead and may become a bottleneck. |
| Maintainability/testability | Fits existing conventions and can be verified with focused concurrency tests where practical. | Easy to write but fails the consistency requirement. | Adds configuration, failure modes, and specialized tests not warranted here. |

**Selected approach.** Prevent capacity overflow by making capacity evaluation and seat allocation an atomic state change. Preserve the starter's transaction/locking pattern. With the no-starter in-memory fallback, synchronize the complete state transition using the simplest mechanism appropriate to its execution model. Duplicate check and active-state write must likewise be atomic.

**Rejected alternatives.** An unsynchronized check-then-write is rejected because concurrent requests can overbook. New distributed coordination or concurrency infrastructure is rejected because one application and one offering do not justify it.

**Rationale and trade-offs.** Atomic allocation prioritizes consistency over the small serialization cost. It follows NFR-01 and preserves the starter pattern as required by NFR-02 when concurrent requests are supported. Focused concurrency testing is practical where the starter supports concurrency.

**Assumptions and consequences.** No starter concurrency model is available. The exact lock/transaction scope, isolation, multi-process guarantees, and implementation/test details remain technology-specific. A single-process in-memory implementation does not coordinate across processes.

### ADR-04 — Duplicate enrollment prevention

**Problem.** A learner must not be enrolled more than once or accumulate duplicate enrollment/waitlist records, including under repeated or concurrent requests.

**Approaches considered**

1. **Check for an active enrollment/waitlist entry and create the new state within the same protected operation; enforce uniqueness in persistence where supported.**
2. **Check for duplicates before writing but without an atomic boundary.**
3. **Silently allow duplicates and attempt cleanup during promotion.**

| Trade-off | Atomic active-registration guard | Unprotected pre-check | Cleanup after duplication |
|---|---|---|---|
| Consistency/concurrency | Enforces at-most-one active state, including races when supported by the store. | Two requests can both see no active registration and create records. | Leaves ambiguous state and can corrupt FIFO and capacity. |
| Performance | Adds a lookup/constraint check; small and necessary for this scope. | Slightly simpler but unsafe. | Cleanup adds more work and failure cases. |
| Maintainability/testability | One invariant can be stated and tested across enrollment and promotion. | Tests may pass serially while concurrent behavior fails. | Requires complicated repair logic and harder tests. |

**Selected approach.** Treat “at most one active registration per learner in the one offering” as an invariant. Check existing active state and make check-plus-write atomic under the selected transaction/locking pattern. Where supported, persistence-level uniqueness is a backstop, not a substitute for clear result handling.

**Rejected alternatives.** An unprotected pre-check is rejected because concurrent requests may both pass it; post-hoc duplicate cleanup is rejected because it can corrupt capacity and FIFO order.

**Rationale and trade-offs.** The atomic guard directly satisfies FR-05, BR-01, and AC-03/AC-09. It adds a lookup/constraint check, but maintains a testable invariant under retries and concurrency.

**Assumptions and consequences.** The starter determines how active state is represented. Return clear, distinguishable outcomes for already enrolled and already waitlisted requests where the existing result convention permits; never create another active record.

### ADR-05 — FIFO waitlist representation

**Problem.** The system must retain FIFO order and identify the earliest eligible learner when a seat opens.

**Approaches considered**

1. **Use the starter's existing ordered queue representation.**
2. **Maintain a deterministic enqueue sequence/position** in the existing persistence or in-memory model.
3. **Sort by wall-clock timestamp alone.**

| Trade-off | Existing order / explicit sequence | Timestamp alone |
|---|---|---|
| Consistency | Can provide a deterministic order, including for entries created at nearly the same time. | Timestamp ties, precision differences, or clock behavior can make order ambiguous. |
| Concurrency | Must allocate order atomically using the starter's existing pattern. | Concurrent timestamps still do not guarantee a unique FIFO position. |
| Performance | Simple ordered retrieval; position re-numbering strategy depends on storage. | Sorting is simple but does not solve ties or enqueue-order definition. |
| Maintainability/testability | FIFO can be asserted directly using insertion sequence/order. | Tests may depend on timing and become flaky. |

**Selected approach.** Preserve the starter's queue ordering if available. Otherwise, define deterministic insertion order (such as a sequence/position or ordered in-memory collection) and add eligible entries at the tail. Do not prescribe a database schema or rely on timestamps alone.

**Rejected alternative.** Wall-clock timestamps alone are rejected unless the starter guarantees unique, deterministic ordering, because ties and clock precision can make order ambiguous.

**Rationale and trade-offs.** Deterministic ordering realizes BR-04 and supports reliable FIFO tests with no new queue infrastructure. Queue position allocation must participate in the atomic state change if concurrent requests can enqueue simultaneously.

**Assumptions and consequences.** Position storage and concurrent sequence allocation are implementation-specific. Gaps or re-numbering are not business requirements as long as relative order remains deterministic.

### ADR-06 — Eligibility and waitlist promotion policy

**Problem.** Promotion must select the earliest eligible learner, and an ineligible learner must not be enrolled. The assignment explicitly requires documenting what happens when the queue head is ineligible but does not decide the policy.

**Approaches considered**

1. **Skip an ineligible candidate for the current promotion attempt, retain their waitlist entry and position, and continue scanning until an eligible candidate is found or the queue is exhausted.**
2. **Remove an ineligible candidate from the waitlist when encountered, then continue.**
3. **Stop at the ineligible head and retain it there, blocking later candidates.**

| Trade-off | Skip and retain | Remove and continue | Stop and block |
|---|---|---|---|
| FIFO/eligibility | Promotes the earliest currently eligible candidate while preserving the ineligible learner's original place. | Promotes the earliest eligible remaining candidate but permanently forfeits the ineligible learner's place. | Preserves strict head order but does not promote the earliest eligible learner behind an ineligible head. |
| Consistency | Requires scanning without removing skipped entries and committing one eligible promotion. | Queue mutation is simple but may discard a learner based on possibly temporary eligibility. | Simple queue operation, but a released seat can remain unused while eligible learners wait. |
| Performance | May recheck multiple candidates and repeat for later drops; bounded by queue length for each attempt. | Similar scan cost, with fewer entries remaining. | Low scan cost but poor seat utilization. |
| Maintainability/testability | Behavior is explicit and testable; retains a learner pending future eligibility changes. | Easy to test but removal semantics need product approval. | Easy to implement, conflicts with the stated “earliest eligible” intent when later candidates qualify. |

**Selected approach.** Revalidate prerequisite eligibility when allocating a seat to a waitlisted learner. Scan entries in deterministic FIFO order; skip an ineligible learner for the current allocation, retain the entry at its original position, and continue until the earliest eligible candidate is found or the queue is exhausted. Promote only the earliest eligible candidate.

**Rejected alternatives.** Removing an ineligible entry is rejected because it permanently removes the learner based on a possibly temporary eligibility result. Stopping at and retaining an ineligible head is rejected because it blocks later eligible learners, contrary to selecting the earliest eligible candidate.

**Rationale and trade-offs.** Revalidation prevents promotion of someone who is no longer eligible. Scanning may evaluate multiple candidates, but each attempt is bounded by the queue length; retaining entries preserves their place and lets them be considered again later. The selected policy is an implementation decision that resolves the assignment's explicit ambiguity; it is not misrepresented as originally specified by the assignment.

**Available-seat priority.** When a new enrollment request is processed while a seat is available, allocate available seats to existing eligible waitlisted learners first. Revalidate and scan in FIFO order as above; only after existing eligible waiters are served may the new learner take a remaining seat. If the offering is still full, enqueue the new eligible learner at the tail. This favors existing queue members and may mean a new learner waits despite a seat having been open at the start of that request.

**Selected design assumption interpreting FR-02 / AC-01.** The assignment's successful-enrollment condition is preserved: an eligible learner is enrolled when capacity remains for that learner after applying the selected priority for existing eligible waitlisted learners. FR-02 and AC-01 remain unchanged; this ordering rule clarifies how the application allocates available capacity and does not change the eligibility or capacity requirements. A newcomer is not denied enrollment solely because a waitlist exists: if no existing waiter is eligible, or capacity remains after eligible waiters are promoted, the newcomer can take a seat.

| Trade-off | Existing eligible waiters first (selected) | New requester first | Remove ineligible candidates |
|---|---|---|---|
| Consistency/FIFO | Honors queue precedence and preserves positions of currently ineligible learners. | Can bypass learners already waiting, violating the selected priority rule. | Frees queue positions but discards a waitlisted learner's place based on a possibly temporary eligibility result. |
| Concurrency | Requires serializing queue allocation and the incoming request decision. | Simpler request path but can starve or reorder existing waiters. | Similar scanning, with additional persistent queue mutation. |
| Performance | May revalidate multiple entries before serving the request; bounded by queue length. | Fewer operations for the new request. | May shorten later scans but incurs removal and possible position maintenance. |
| Maintainability/testability | Priority and retained positions are explicit, deterministic test cases. | Simple but conflicts with selected policy and causes fairness ambiguity. | Testable but requires defining permanent removal/re-entry behavior not selected. |

**Assumptions and consequences.** Retained ineligible entries may be reconsidered on later seat allocations. If no waitlisted candidate is eligible, leave the seat available after a successful drop; a later eligible request may take it. The eligibility source and freshness remain technology/business-data questions.

### ADR-07 — Transaction boundaries

**Problem.** Enrollment/waitlisting, drops, and promotion each change shared state. A partial drop/promotion could leave a seat unused or a learner in neither/both states.

**Approaches considered**

1. **Treat enrollment as one logical transaction and treat successful drop plus one promotion attempt as one logical workflow, using a single atomic transaction where supported.**
2. **Commit the drop first, then promote in a separate operation.**
3. **Use compensating actions or a distributed transaction across separate services/stores.**

| Trade-off | One supported atomic boundary | Drop then promote separately | Compensation/distributed transaction |
|---|---|---|---|
| Consistency | Strongest state transition when the existing persistence supports it. | A crash/failure between steps can leave an open seat despite a queue or partially updated state. | Can coordinate separate resources but adds substantial complexity. |
| Concurrency | Can serialize related seat and queue changes using starter mechanisms. | Requires careful race handling between drop and promotion. | Requires distributed coordination and more failure modes. |
| Performance | May hold a transaction/lock longer while eligibility and candidate selection occur. | Shorter individual operations but needs recovery/reconciliation behavior. | Highest coordination overhead. |
| Maintainability/testability | One workflow and invariant boundary are easier to test. | Needs explicit partial-failure tests and recovery semantics. | Excessive for one bounded application unless starter requires it. |

**Selected approach.** Define enrollment, including existing-waitlist priority allocation and the new learner's resulting enrollment/waitlist decision, as one atomic state change. Define successful drop plus FIFO eligibility revalidation and promotion as one logical workflow; make it atomic where the starter supports the boundary. If the starter cannot span the full drop/promotion workflow, preserve its supported atomic operations and ensure a persisted drop leaves the seat available and no learner in a partially changed enrollment/waitlist state.

**Rejected alternatives.** Committing a drop and then allowing an unhandled partial queue/enrollment mutation is rejected because it can corrupt state. Distributed transactions and compensation across separate services/stores are rejected as disproportionate and unsupported.

**Rationale and trade-offs.** Atomic state changes simplify capacity, duplicate, queue-order, and enrollment invariants. A longer supported transaction may reduce concurrency, but avoids partial registration transitions. Preserve the existing starter pattern rather than introducing a transaction manager or infrastructure.

**Assumptions and consequences.** Exact transaction scope and failure recovery depend on starter persistence and must be documented. If promotion cannot be committed with the drop, a successful drop may complete with its seat open; a failed promotion must not remove a waitlist entry without creating its enrollment. Do not report a promotion that did not persist.

### ADR-08 — Error and outcome handling

**Problem.** Learners need clear results for success, waitlisting, ineligibility, duplicate requests, invalid requests, and repeated drops, while technical failures must not appear successful.

**Approaches considered**

1. **Represent expected business outcomes explicitly in the starter's result/return convention; reserve exceptions or error paths for unexpected technical failures.**
2. **Use exceptions for all business outcomes and infrastructure failures.**
3. **Return a generic success/failure value with no meaningful distinction.**

| Trade-off | Explicit outcomes + technical errors | Exceptions for all | Generic result |
|---|---|---|---|
| Consistency/clarity | Distinguishes ineligible, waitlisted, duplicate, and not-enrolled states without implying corruption. | Can conflate normal decisions with exceptional failures. | Does not satisfy clear-result expectations. |
| Performance | Minimal overhead; normal branches remain ordinary control flow. | Runtime-specific overhead and exception flow for routine conditions. | Efficient but insufficiently informative. |
| Maintainability/testability | Tests can assert business results separately from failure paths. | Tests and callers must interpret exception types for normal workflow. | Hard to verify acceptance criteria precisely. |
| Compatibility | Can map to starter's API, CLI, or service conventions. | May conflict with existing error conventions. | Likely conflicts with expected detail. |

**Selected approach.** Return clear outcomes using the starter's existing convention for successful enrollment, waitlisting, duplicate enrollment requests, repeated/invalid drops, and invalid requests. Surface persistence/configuration/transaction failures through the starter's standard error path; do not swallow them or report success after failed writes.

**Rejected alternatives.** Using exceptions for routine business outcomes is rejected when it obscures normal decisions. A generic success/failure response is rejected because it does not clearly distinguish duplicate, repeated-drop, and invalid-request outcomes.

**Rationale and trade-offs.** Clear expected outcomes make behavior independently verifiable and preserve the assignment's requested user feedback. Exact outcome names/messages/status codes follow starter conventions and are not prescribed here.

**Assumptions and consequences.** Repeated drop may use a `NOT_ENROLLED`-style outcome that clearly states no active enrollment existed; no separate historical record is required solely to distinguish a repeated drop.

### ADR-09 — Prerequisite rule and data source

**Problem.** Eligibility is mandatory for enrollment and promotion, but neither the prerequisite rule nor its authoritative data source is described.

**Approaches considered**

1. **Use the prerequisite rule and learner data already provided by the starter.**
2. **Use a small local rule/data representation only if required by a no-starter prototype and explicitly documented.**
3. **Integrate a new identity, student-record, or external prerequisite service.**

| Trade-off | Starter data / documented minimal fallback | New external integration |
|---|---|---|
| Correctness | Follows the available source of truth; a fallback makes assumptions visible. | Could provide authoritative data but no such system or contract is identified. |
| Concurrency/performance | Local evaluation can remain in the application workflow; actual characteristics depend on starter. | Adds network availability, latency, retries, and consistency concerns. |
| Complexity/testability | Focused validator tests can use the existing model or a simple explicit fixture. | Requires client configuration, integration tests, and failure semantics. |
| Scope | Fits one bounded feature. | Risks adding prohibited/unrequested identity integration. |

**Selected approach.** Use the prerequisite rule and eligibility data already provided by the starter. If no starter implementation exists and an eligibility rule must be supplied for a runnable prototype, explicitly document the minimal rule/data assumption rather than presenting it as an assignment requirement. Revalidate eligibility immediately before promotion.

**Rejected alternative.** A new identity, student-record, or external prerequisite integration is rejected because no such system is named or required and external identity integrations are out of scope.

**Rationale and trade-offs.** Reusing starter data avoids integration complexity and keeps validation testable within the application. Revalidation means eligibility can change between waiting and seat allocation; this is intentional to prevent promotion of an ineligible learner.

**Assumptions and consequences.** The actual rule, authoritative data, and behavior when eligibility data is unavailable remain unresolved. A test stub is not a real external service or a defined prerequisite rule.

## 4. Cross-decision consistency

- The selected single application and persistence boundary retain all capacity and queue invariants within the scope of the existing C4 diagrams.
- Starter stack and persistence always take precedence; recommendations do not authorize replacing them with preferred technologies.
- The in-memory option is a fallback only when no starter implementation exists. It does not imply durability or multi-instance coordination.
- FIFO means the earliest currently **eligible** candidate is considered for promotion; ineligible entries are skipped for that allocation and retained at their original positions.
- When allocating available seats, existing eligible waitlisted learners take priority over new enrollment requests.
- If no queued learner is eligible, the successful drop completes and the seat remains available.
- No frontend, course catalog, payments, identity integration, multiple offerings/terms, deployment architecture, new database, configurable policy engine, or separate microservice is introduced.

## 5. Unresolved decisions and information needed

| Decision | Information needed |
|---|---|
| Starter stack and existing architecture | Starter source, build/dependency files, current interfaces, data model, persistence code, transaction/locking pattern, and test conventions. |
| Prerequisite rule | Exact eligibility rule and authoritative data available in the starter; behavior when that data is missing or unavailable. |
| Capacity | Capacity value and how it is configured or represented by the starter. |
| Concurrency model | Whether requests can be concurrent, single- or multi-process assumptions, and guarantees supported by existing transactions/locks. |
| Transaction support | Whether the starter can atomically commit drop plus promotion; if not, its supported operations and failure behavior. |
| Request/result conventions | Whether the starter interaction is a service or CLI, input validation rules, and expected result/error representation. |
| Eligibility data failure | Behavior if the starter's eligibility data cannot be read or evaluated. |

## 6. Validation against source documents

The decisions preserve the one-offering scope, eligibility checks, FIFO ordering, no-duplicate and capacity invariants, successful drop, starter compatibility, conditional concurrency expectations, and prohibition on unnecessary infrastructure. The selected retain-and-skip policy resolves the assignment's queue-head ambiguity; the implementation must record and apply it consistently. Drop with no eligible candidate completes and leaves the seat open.

**Consistency note:** FR-02 and AC-01 are preserved as written. For implementation, “capacity remains” means capacity remaining after existing eligible waitlisted learners have first been considered under ADR-06. Focused examples in the LLD verify this ordering, the retained positions of ineligible waiters, and the outcome when no eligible waiter is found.
