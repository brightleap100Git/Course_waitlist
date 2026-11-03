# Course Waitlist Requirements Specification

## 1. Document Overview

### Purpose

Translate the Course Waitlist AI-Assisted Assessment into a traceable, implementation-ready specification for the scoped course-registration feature.

### Scope

This specification covers enrollment in one course offering, dropping an enrolled learner, and promoting eligible learners from that offering's waitlist. It describes the assignment's explicit requirements and separates unresolved assumptions and implementation choices from those requirements.

### Objectives

- Preserve the assignment's bounded service-slice scope.
- Define verifiable behavior for enrollment, capacity, prerequisites, duplicate requests, drops, and waitlist promotion.
- Provide a foundation for design, implementation, and focused automated tests without prescribing a programming language, API, framework, or database.

## 2. Business Context

### Problem statement

A course offering has limited capacity. Learners need to enroll when eligible, join a waitlist when the offering is full, and be considered for enrollment when an enrolled learner drops.

### Business need

The registration workflow must enforce capacity and prerequisite eligibility, avoid duplicate learner records, and handle waitlist promotion in a predictable order. It must do so as a reliable, bounded feature rather than a complete registration platform.

### Expected outcome

A runnable implementation and focused automated tests demonstrate the scoped workflow. A proportionate design specification, AI-use record, setup and test instructions, and a concise account of limitations or follow-up risks accompany the implementation.

## 3. Actors and Responsibilities

| Actor | Responsibilities supported by the assignment |
|---|---|
| Learner | Requests enrollment and may drop an existing enrollment. The learner must satisfy the applicable prerequisite rule to be enrolled or promoted. |
| Registration service or prototype | Validates requests and eligibility, enforces offering capacity, manages enrollment and waitlist state, and applies the documented promotion policy. This is the system under development, not an additional user role. |

No instructor, administrator, identity provider, or other actor is specified.

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-01 | The system shall support enrollment for one course offering. |
| FR-02 | The system shall enroll a learner when capacity is available and the learner satisfies the prerequisite rule used by the implementation. |
| FR-03 | The system shall not enroll an ineligible learner. |
| FR-04 | When the offering is full, the system shall allow an eligible learner to join its waitlist. |
| FR-05 | A learner shall not be enrolled more than once. Repeated enrollment requests shall not create duplicate enrollment or waitlist records. |
| FR-06 | The system shall allow an enrolled learner to drop the enrollment. A successful drop shall release a seat. |
| FR-07 | When a seat becomes available, the system shall consider waitlisted learners in FIFO order and promote the earliest eligible learner according to a documented eligibility policy. |
| FR-08 | The system shall not promote an ineligible learner. The system shall apply the selected, documented policy consistently when an ineligible learner is encountered during promotion. |
| FR-09 | Invalid enrollment or drop requests, and repeated drops, shall return clear results and shall not corrupt enrollment, capacity, or waitlist state. |
| FR-10 | The system shall use the starter project's data model, interfaces, persistence, and conventions when they are supplied. If no starter implementation is supplied, a small service or command-line prototype with an in-memory store is sufficient. |

## 5. Business Rules

These rules restate assignment requirements. Where the assignment does not define a value or policy, the gap is called out rather than resolved here.

| ID | Rule |
|---|---|
| BR-01 | A learner may be enrolled only once in the scoped offering. |
| BR-02 | Enrollment requires available capacity and satisfaction of the applicable prerequisite rule. |
| BR-03 | When capacity is exhausted, an eligible learner may wait instead of being enrolled. |
| BR-04 | Waitlist position is FIFO: earlier waitlisted eligible learners have promotion priority over later eligible learners. |
| BR-05 | Promotion must not enroll an ineligible learner. Whether an ineligible learner at the front is skipped or retained, and how the queue is then processed, is unresolved and must be documented by the implementation. |
| BR-06 | Dropping an enrolled learner releases a seat and makes promotion possible. |
| BR-07 | Invalid requests and duplicate or repeated requests must not create duplicate records or leave enrollment, capacity, or waitlist state corrupted. |

The assignment does not give a numeric capacity, define the prerequisite relation, or specify the exact classification of invalid requests.

## 6. Assumptions and Clarifications

### Explicit assignment requirements

- The feature is for one course offering.
- Eligibility depends on a prerequisite rule, but the assignment does not define the rule's data or evaluation details.
- Waitlist ordering is FIFO, and promotion is to the earliest eligible learner.
- The implementation must state its prerequisite and waitlist assumptions, including how it handles an ineligible learner at the front of the queue.
- No configurable policy engine is to be built.

### Assumptions

This specification does not silently select a prerequisite model, ineligible-learner policy, capacity value, or invalid-request taxonomy. Any assumptions needed to implement those details must be stated in the design specification and applied consistently.

### Unresolved questions

1. What prerequisite rule applies, and what data is authoritative for determining whether a learner satisfies it?
2. If the learner at the front of the waitlist is ineligible when a seat opens, is that learner skipped for this promotion, removed from the queue, or retained at the front? What happens to subsequent learners?
3. At what points must eligibility be checked (for example, on initial request and again at promotion)? The assignment requires an ineligible learner not be enrolled or promoted but does not define eligibility-data timing.
4. What capacity value is used, and where does it come from in the starter project?
5. Which request conditions count as invalid, and what response/result format does the starter project require?
6. If a learner is already enrolled and submits another enrollment request, or is already waitlisted and submits another request, should the result distinguish those cases? Duplicate records must not be created, but the exact result is unspecified.
7. If a successful drop releases a seat but no eligible learner can be promoted, should the seat simply remain available? The assignment does not specify a more detailed outcome.

### Design decisions deferred to architecture and implementation

- Service boundary and relevant data/API design.
- Representation of capacity, enrollments, prerequisites, and waitlist entries, subject to starter-project compatibility.
- Exact validation and result/error representation.
- The documented ineligible-learner queue policy.
- Transaction or locking approach, using the starter's existing pattern where applicable.
- Persistence approach, subject to the starter-project constraint and the permitted in-memory fallback.

## 7. Non-Functional and Technical Constraints

| ID | Constraint |
|---|---|
| NFR-01 | State changes must preserve enrollment, capacity, and waitlist consistency; invalid and duplicate operations must not corrupt state. |
| NFR-02 | If the starter project uses concurrent requests, preserve its transaction or locking pattern. |
| NFR-03 | Add a focused concurrency test where practical when the starter uses concurrent requests. The assignment does not impose a particular concurrency architecture or guarantee beyond this conditional expectation. |
| NFR-04 | Use the starter project's persistence and data model when supplied. If there is no starter implementation, an in-memory store is acceptable for the minimal service or CLI prototype. Do not add a new database unless the starter requires it. |
| NFR-05 | Work within the supplied starter's interfaces and test conventions when present. Do not create separate greenfield, brownfield, and ambiguous implementations. |

The assignment does not specify a language, framework, database technology, API style, deployment target, or quantitative performance/reliability target.

## 8. Acceptance Criteria

| ID | Independently verifiable criterion |
|---|---|
| AC-01 | Given an offering with a free seat and an eligible learner, an enrollment request succeeds and the learner occupies no more than one seat. |
| AC-02 | Given an offering at capacity and an eligible learner who is not already enrolled or waitlisted, the request places that learner on the waitlist rather than over capacity. |
| AC-03 | Repeating an enrollment request for the same learner does not create duplicate enrollment or waitlist records. |
| AC-04 | Given an enrolled learner and a non-empty waitlist, dropping the enrollment releases a seat and promotes the earliest eligible waitlisted learner according to the policy documented by the implementation. |
| AC-05 | If an ineligible learner is encountered during enrollment or promotion, that learner is not enrolled; the documented waitlist policy is applied consistently when applicable. |
| AC-06 | A learner who fails the applicable prerequisite rule cannot enroll or be promoted. |
| AC-07 | Invalid enrollment and drop requests return clear results and leave enrollment, capacity, and waitlist state consistent. |
| AC-08 | Dropping a learner who is not enrolled, including repeating a prior drop, returns a clear result and does not corrupt state. |
| AC-09 | After successful, invalid, duplicate, and repeated operations, state remains consistent: enrollment does not exceed capacity, no learner has duplicate enrollment/waitlist records, and waitlist order follows the documented policy. |
| AC-10 | Focused automated tests cover available capacity, full capacity, duplicate requests, FIFO promotion, prerequisite failure, invalid requests, and invalid or repeated drops. |
| AC-11 | If the starter supports concurrent requests, the existing transaction or locking pattern is preserved and a focused concurrency test is added where practical. |
| AC-12 | AI is used for at least one engineering activity; prompts, relevant response excerpts or summaries, suggestion disposition, affected files or decisions, and verification evidence are recorded. |

## 9. Test Requirements

Tests should use the starter project's test conventions when available and verify resulting state as well as returned results.

### Functional scenarios

- Enroll an eligible learner when capacity remains.
- Reject enrollment when the learner does not meet the applicable prerequisite rule.
- At full capacity, place an eligible learner on the waitlist without exceeding capacity.
- Verify that an ineligible learner is neither enrolled nor promoted.
- Drop an enrolled learner and verify seat release and promotion behavior.
- Verify FIFO promotion among eligible waitlisted learners.
- Submit duplicate enrollment requests for an enrolled learner and for a waitlisted learner; verify no duplicate records.
- Submit invalid enrollment/drop requests and repeat a drop; verify clear results and unchanged consistent state.

### Boundary and negative scenarios

- Enrollment when capacity is exactly available and when the offering is full.
- Drop when no learner is enrolled, and drop the same enrollment more than once.
- Promotion with an empty waitlist.
- Promotion when the first waitlisted learner is ineligible, exercising the policy selected and documented by the implementation.
- Requests made by a learner whose eligibility fails the stated prerequisite rule.

### Waitlist ordering and state scenarios

- Enqueue multiple eligible learners and verify their order is retained.
- After a drop, verify that promotion selects the earliest eligible learner under the documented ineligible-learner policy.
- Verify after each success and failure that enrollment does not exceed capacity and duplicate records are absent.

### Concurrency

If the starter supports concurrent requests, test the relevant concurrent enrollment/drop behavior where practical and verify that the starter's transaction or locking pattern prevents inconsistent capacity or queue state.

## 10. Scope Boundaries

### In scope

- Enrollment, dropping, and waitlist promotion for one course offering.
- Capacity and prerequisite eligibility checks.
- Duplicate-request handling and clear handling of invalid or repeated drop requests.
- FIFO ordering and a documented policy for ineligible learners during promotion.
- Focused automated tests and a proportionate design specification.

### Out of scope

- A complete registration product or support for multiple course offerings.
- A frontend or course catalog.
- Payments or external identity integrations.
- Multi-term registration.
- Deployment.
- A new database unless required by the starter project.
- Separate greenfield, brownfield, and ambiguous implementations.
- A configurable policy engine.

## 11. AI-Assisted Engineering Requirements

- Use AI for at least one engineering activity.
- Record the prompts used and relevant AI response excerpts or summaries.
- For each relevant suggestion, record whether it was accepted, edited, or rejected.
- Identify the files or decisions informed by AI suggestions.
- Independently review and verify AI-assisted work; include verification evidence.
- Do not include credentials, secrets, or unnecessary sensitive data in prompts or submitted prompt records.
- Describe AI contributions candidly and prioritize independent review over unverified AI output.

## 12. Expected Deliverables

| ID | Deliverable |
|---|---|
| D-01 | Runnable code implementing the scoped feature. |
| D-02 | Focused automated tests for the specified workflow and acceptance criteria. |
| D-03 | A proportionate design specification covering the service boundary, relevant data/API design, enrollment and capacity invariants, waitlist-promotion rule, concurrency or persistence approach, and key assumptions or tradeoffs. |
| D-04 | An AI prompt and use record with prompts, relevant response excerpts or summaries, accepted/edited/rejected dispositions, affected files or decisions, and verification evidence. |
| D-05 | Brief setup and run instructions, including commands used to execute tests. |
| D-06 | A concise summary of in-scope limitations and follow-up risks. |

## 13. Execution Guidelines

### Suggested implementation sequence

1. Inspect the supplied project, its interfaces, data model, persistence, and test conventions.
2. Record prerequisite and waitlist assumptions; resolve or explicitly document the open policy decisions before implementation.
3. Specify service boundary, data/API design, invariants, and concurrency or persistence approach proportionate to the feature.
4. Implement enrollment, duplicate handling, drops, and documented FIFO promotion behavior.
5. Add and run focused functional, boundary, and negative tests; add a concurrency test where practical if the starter supports concurrent requests.
6. Independently review AI-assisted suggestions and record their disposition and verification evidence.
7. Provide setup, run, and test instructions plus limitations and follow-up risks.

### Suggested timebox

The assignment suggests 7–8 hours of effort, focused on the scoped workflow, tests, design specification, and AI-use record. This is a suggested effort, not a functional requirement.

### Quality expectations

- Prioritize correct capacity and queue behavior, focused tests, understandable changes, and evidence of independent review.
- Keep design and implementation proportionate to this bounded feature; do not add extra infrastructure or feature scope.
- Submit the implementation and all listed deliverables together so the work can be run and reviewed.
