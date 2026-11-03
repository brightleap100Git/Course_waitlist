# Proposed Future Production Requirements

## 1. Purpose and status

This document proposes production-grade non-functional requirements (NFRs) for a possible future evolution of the Course Registration and Waitlist Management application. It is a planning document, not an amendment to [the assignment requirements](../requirement.md), finalized ADRs, or the current implementation.

The current application is a Java 21, Maven-based, single-offering command-line prototype. It uses in-memory state and a deterministic demo eligibility fixture. It has no production API, identity integration, database, deployment topology, or operational SLO. Those constraints make the current implementation suitable for its bounded assignment scope, but not a production service.

All numerical targets in this document are **proposed illustrative targets only**. They require representative load testing, operational feasibility review, and business approval before adoption as commitments. No target is an existing requirement or claim about current performance.

### Priority definitions

- **High:** Foundational for a production service that processes real registrations.
- **Medium:** Important for growth, operational maturity, or conditional capabilities.
- **Low:** Adopt when justified by the product's user base, regulatory context, or feature roadmap.

## 2. Proposed non-functional requirements

### FUT-NFR-01 — Scalability and throughput

| Field | Proposal |
|---|---|
| Current implementation and limitation | The application supports one offering in one process, with in-memory state. There is no defined workload, horizontal scaling, or capacity plan. |
| Proposed future requirement | The service should sustain approved peak enrollment and drop traffic without violating capacity, duplicate-registration, or waitlist-order invariants. It should scale its stateless request-processing tier horizontally where the persistence and concurrency design permit. |
| Illustrative measurable SLI/SLO | **Proposed:** Sustain 100 registration state-change requests/second for a 30-minute representative peak test, with at least 99.9% successful processing of valid requests and no invariant violations. Determine final throughput from business forecasts and load testing. |
| Recommended approach | Measure expected registrations per offering and peak burst patterns first. Separate stateless request handling from durable shared state; use indexed persistence and bounded pagination for queue/state reads. Scale application instances only after shared-state concurrency is correctly enforced. |
| Priority | High |
| Key trade-offs | Horizontal application scaling improves throughput and availability but requires shared durable state and distributed coordination. Larger capacity adds cost and can increase contention on popular offerings. |
| Suggested verification | Load-test realistic enrollment/drop mixes, queue lengths, and burst patterns; assert no over-capacity enrollment, duplicates, or FIFO violations and report throughput, saturation, and error rates. |

### FUT-NFR-02 — API performance and latency

| Field | Proposal |
|---|---|
| Current implementation and limitation | There is no network API. CLI response time is local process behavior and does not establish service latency. |
| Proposed future requirement | If a network API is introduced, enrollment, drop, and state-read operations should respond within an approved latency budget under the agreed normal and peak load, without weakening transaction correctness. |
| Illustrative measurable SLI/SLO | **Proposed:** For valid requests at the approved peak load, p95 response latency no greater than 300 ms and p99 no greater than 750 ms; exclude client/network transit only if the measurement boundary is documented. |
| Recommended approach | Establish an API only when required. Define request deadlines, bounded queue queries, pagination for potentially large reads, and latency instrumentation by operation. Avoid remote prerequisite checks inside a long-held state transaction unless measured and required. |
| Priority | High |
| Key trade-offs | Tighter latency objectives can increase infrastructure cost and pressure to shorten or split transactions. Splitting state changes can weaken atomicity and is not acceptable without an explicit consistency design. |
| Suggested verification | Benchmark API endpoints with representative state sizes and contention; validate percentile latency at both normal and peak loads and inspect database/lock wait time. |

### FUT-NFR-03 — High availability

| Field | Proposal |
|---|---|
| Current implementation and limitation | A single local CLI process has no service deployment, health checks, redundancy, or availability objective. Process termination loses current state. |
| Proposed future requirement | A production service should remain available through an individual application-instance failure and recover service access after a zone or infrastructure failure within approved objectives. |
| Illustrative measurable SLI/SLO | **Proposed:** 99.9% monthly service availability, measured at the public service boundary; individual-instance failure should not cause an extended registration outage. Targets require business approval and a defined maintenance policy. |
| Recommended approach | Use multiple stateless application instances behind a health-aware load balancer and a highly available durable data layer. Design graceful shutdown, readiness/liveness checks, and dependency-aware health reporting. |
| Priority | High |
| Key trade-offs | Redundancy increases infrastructure and operational cost. Multi-zone or multi-region designs introduce replication lag, failover complexity, and possible consistency/availability trade-offs. |
| Suggested verification | Run instance termination and dependency-failure exercises; verify health-based routing, recovery time, and preservation of committed registration state. Measure availability over an agreed window. |

### FUT-NFR-04 — Data durability and consistency

| Field | Proposal |
|---|---|
| Current implementation and limitation | The in-memory repository serializes transactions and stages updates before committing them, but state is process-local and lost on restart. It is not shared across instances. |
| Proposed future requirement | Acknowledged registration changes should be durably stored and atomically preserve capacity, uniqueness, enrollment/waitlist exclusivity, and the documented FIFO promotion policy. |
| Illustrative measurable SLI/SLO | **Proposed:** Zero lost acknowledged transactions in routine restart/failover tests; zero invariant violations in automated consistency checks and concurrency/load tests. Durability guarantees should match the selected storage service's documented commit semantics. |
| Recommended approach | Select a durable transactional store compatible with the approved deployment. Model uniqueness and capacity invariants in both application logic and persistence constraints where practical. Define transaction boundaries, schema migration, backup, and restore procedures before production use. |
| Priority | High |
| Key trade-offs | Stronger transaction guarantees can reduce write throughput and increase contention. Database constraints provide defense in depth but may require careful schema design for ordered waitlists and promotion workflows. |
| Suggested verification | Test restart recovery, transaction rollback, concurrent allocation, and backup restoration. Reconcile persisted state against capacity and uniqueness invariants after fault-injection tests. |

### FUT-NFR-05 — Distributed concurrency and capacity enforcement

| Field | Proposal |
|---|---|
| Current implementation and limitation | Synchronized in-memory transactions protect state only inside one repository instance/process. They cannot coordinate multiple processes or hosts. |
| Proposed future requirement | Concurrent requests handled by any service instance must never allocate more seats than capacity, create duplicate active registrations, place a learner in both enrolled and waitlisted state, or violate the selected waiter priority/order. |
| Illustrative measurable SLI/SLO | **Proposed:** Under a burst of at least 1,000 simultaneous requests competing for the final seats, persisted enrollment never exceeds capacity and each learner has at most one active registration. Test target size is illustrative and subject to load-test design. |
| Recommended approach | Enforce allocation through a single durable transaction using an appropriate isolation/locking or atomic conditional-update strategy. Add database uniqueness constraints or equivalent enforcement for active learner registrations. Use idempotency keys for retryable API writes if an API is adopted. Avoid relying on process-local locks for cross-instance correctness. |
| Priority | High |
| Key trade-offs | Pessimistic locking can serialize high-contention offerings; optimistic concurrency may require retries and careful idempotency. Distributed locks add failure modes and should not replace data-store invariants without a strong reason. |
| Suggested verification | Multi-instance concurrency tests against the selected persistence, including final-seat races, duplicate retries, simultaneous drops/promotions, deadlock/retry behavior, and invariant assertions against committed state. |

### FUT-NFR-06 — Security and authorization

| Field | Proposal |
|---|---|
| Current implementation and limitation | The CLI accepts caller-supplied learner IDs and uses demo eligibility IDs. There is no authentication, authorization, transport security, secret management, or production learner identity source. |
| Proposed future requirement | Any production interface must authenticate callers, authorize each operation for the relevant learner or administrative role, protect data in transit and at rest, validate untrusted input, and prevent unauthorized enrollment, drop, or state disclosure. |
| Illustrative measurable SLI/SLO | **Proposed:** 100% of externally reachable state-changing operations require authentication and authorization; automated access-control tests cover every role/operation combination; no critical or high severity security findings remain open at release approval. |
| Recommended approach | Integrate with an approved identity provider only after product and security approval. Use least-privilege roles, TLS, managed secrets, secure logging, dependency scanning, and a documented threat model. Keep authorization at the service boundary and enforce ownership checks in business operations. |
| Priority | High |
| Key trade-offs | Authentication and authorization add integration and operational dependencies. Fine-grained policies improve control but can increase implementation and review complexity. Security controls must not expose sensitive eligibility details in errors or logs. |
| Suggested verification | Threat modeling, code/dependency scanning, penetration testing of the approved interface, and automated tests for unauthenticated, unauthorized, cross-learner, and privilege-escalation attempts. |

### FUT-NFR-07 — Observability and alerting

| Field | Proposal |
|---|---|
| Current implementation and limitation | The CLI displays command results, but the application has no production metrics, structured logs, traces, dashboards, or alerting. |
| Proposed future requirement | Operators should be able to detect and diagnose service failures, elevated latency, capacity contention, promotion failures, and dependency degradation without logging unnecessary personal data. |
| Illustrative measurable SLI/SLO | **Proposed:** Emit request count, result category, latency, error rate, queue depth, promotion outcomes, and dependency health metrics; alert on sustained SLO burn or a material increase in failed state changes within five minutes. Thresholds require operational tuning. |
| Recommended approach | Adopt structured, correlation-ID-based logs and standard metrics/tracing instrumentation. Use dashboards and actionable alerts tied to runbooks. Restrict metric labels to bounded-cardinality values and redact learner identifiers and sensitive eligibility data. |
| Priority | High |
| Key trade-offs | More telemetry improves diagnosis but incurs storage, processing, and privacy costs. High-cardinality identifiers and excessive event detail can harm cost and confidentiality. |
| Suggested verification | Validate telemetry completeness and redaction in integration tests; inject errors and latency to confirm dashboards and alerts fire and link to accurate runbooks. |

### FUT-NFR-08 — Resilience and disaster recovery

| Field | Proposal |
|---|---|
| Current implementation and limitation | There is no persistent backup, recovery procedure, deployment failover, or defined recovery objective. Restart resets all in-memory demo state. |
| Proposed future requirement | The production service should recover from application, data-store, and infrastructure failures within business-approved recovery objectives, with a tested procedure for restoring registration state. |
| Illustrative measurable SLI/SLO | **Proposed:** Recovery time objective (RTO) of one hour and recovery point objective (RPO) of five minutes for committed registration data. Both values are provisional and require business approval, storage capabilities, and disaster-recovery exercises. |
| Recommended approach | Define backup frequency, point-in-time recovery, encryption, retention, restoration ownership, and failover procedures. Maintain infrastructure-as-code and tested runbooks. Ensure recovery does not silently duplicate promotion or lose acknowledged state. |
| Priority | High |
| Key trade-offs | Lower RTO/RPO targets require more frequent backups, replication, and operational investment. Synchronous replication can increase latency; asynchronous replication can lose recent writes during failover. |
| Suggested verification | Perform scheduled restore drills and fault-injection/failover exercises; measure actual RTO/RPO and validate enrollment, waitlist, and capacity invariants after recovery. |

### FUT-NFR-09 — Maintainability and CI/CD

| Field | Proposal |
|---|---|
| Current implementation and limitation | The project has a small Java/Maven build and automated JUnit/JaCoCo checks. It has no defined production release pipeline, deployment automation, environment promotion, or rollback process. |
| Proposed future requirement | Changes should be reproducibly built, tested, reviewed, and deployed through an auditable pipeline, with backward-compatible data changes and a practical rollback or roll-forward strategy. |
| Illustrative measurable SLI/SLO | **Proposed:** 100% of production releases originate from a green, reviewed CI pipeline; all required unit, integration, concurrency, and security checks pass; deployment rollback or recovery is exercised at least quarterly. |
| Recommended approach | Retain standard Maven build conventions; add CI stages for compilation, tests, coverage thresholds, static analysis, dependency/security checks, artifact publication, and controlled deployment. Version database migrations and use staged rollout/feature controls only when justified. |
| Priority | High |
| Key trade-offs | Additional gates improve confidence but increase pipeline time and maintenance. Strict coverage gates can incentivize unhelpful tests unless paired with meaningful behavioral review. |
| Suggested verification | Validate pipeline behavior using intentionally failing tests/checks, inspect artifact provenance, test migration compatibility, and exercise deployment rollback in a non-production environment. |

### FUT-NFR-10 — Auditability and compliance

| Field | Proposal |
|---|---|
| Current implementation and limitation | The application does not persist an audit history. Console output is not a durable, access-controlled audit record. No applicable regulatory or retention obligations are established. |
| Proposed future requirement | If business, policy, or legal needs require traceability, record material registration state changes with actor, action, timestamp, outcome, and correlation reference; protect records against unauthorized alteration and define retention/access policies. |
| Illustrative measurable SLI/SLO | **Proposed:** 100% of committed enroll, waitlist, drop, and promotion state changes have a corresponding audit record; audit persistence is transactionally coupled or otherwise reliably reconciled. Retention period and review cadence require legal/business approval. |
| Recommended approach | Define an audit schema and data classification with privacy/legal stakeholders. Use append-only or access-controlled audit storage, synchronized timestamps, least-privilege access, retention/deletion controls, and a reliable transactional-outbox or equivalent publication pattern if records are stored separately. |
| Priority | Medium |
| Key trade-offs | Detailed audit trails aid investigation but increase storage and privacy exposure. Immutable retention can conflict with data minimization or deletion obligations; applicable obligations must be determined rather than assumed. |
| Suggested verification | Test audit completeness for successful, rejected, and rolled-back operations; review access controls, retention behavior, tamper evidence, and privacy redaction. |

### FUT-NFR-11 — Accessibility (conditional on a user interface)

| Field | Proposal |
|---|---|
| Current implementation and limitation | The current interface is a text CLI, not a graphical or browser UI; no accessibility conformance assessment is documented. |
| Proposed future requirement | If a graphical or web user interface is introduced, core registration and waitlist workflows should be operable and understandable for users with disabilities, including keyboard and assistive-technology users. |
| Illustrative measurable SLI/SLO | **Proposed:** Meet WCAG 2.2 AA for applicable user-interface content and workflows, subject to product/legal approval; no critical keyboard or screen-reader blockers in release testing. |
| Recommended approach | Include accessibility in design and acceptance criteria from the start. Use semantic controls, keyboard-operable workflows, visible focus, accessible validation/error announcements, sufficient contrast, and representative assistive-technology testing. |
| Priority | Medium (conditional) |
| Key trade-offs | Early accessibility work reduces exclusion and rework; evaluation and remediation require design and testing effort. Automated scanners alone cannot establish conformance. |
| Suggested verification | Automated accessibility scans plus manual keyboard, screen-reader, zoom/reflow, and contrast review of end-to-end enrollment, drop, and error flows. |

### FUT-NFR-12 — Event-driven notification and integration reliability

| Field | Proposal |
|---|---|
| Current implementation and limitation | There are no outbound notifications, event bus, external integrations, or delivery/retry mechanisms. The assignment explicitly excludes external identity integrations and does not require notifications. |
| Proposed future requirement | **Conditional:** If approved notification or downstream integration capabilities are introduced, state-changing events should be delivered reliably without compromising registration transaction consistency. Consumers should tolerate duplicate and out-of-order delivery as defined by the event contract. |
| Illustrative measurable SLI/SLO | **Proposed:** At least 99.9% of accepted notification events are delivered to the configured provider within five minutes, with failures surfaced for retry or operator action and no lost committed event in tested failure scenarios. This is conditional and requires product/provider approval. |
| Recommended approach | Only after approving the capability, use a transactional outbox or equivalent durable handoff, idempotent consumers, bounded exponential backoff, dead-letter handling, delivery metrics, and versioned event schemas. Avoid synchronous external calls inside enrollment/drop transactions. |
| Priority | Low (conditional on capability approval) |
| Key trade-offs | Asynchronous delivery isolates registration from provider outages but introduces eventual consistency, duplicate delivery, ordering concerns, and additional operations. |
| Suggested verification | Contract-test event schemas; inject broker/provider downtime, timeouts, duplicates, and retries; verify no event is silently lost and registration state remains correct. |

## 3. NFRs versus new functional capabilities

The requirements above primarily describe quality attributes and operational characteristics. The following items are **new functional capabilities**, not NFRs; they require separate product scope, user stories, business rules, acceptance criteria, and approval before implementation:

| Capability | Why it is a separate functional scope |
|---|---|
| Multiple courses, offerings, or terms | Expands the current one-offering registration model and changes identity, capacity, queue, and conflict rules. |
| Learner accounts, staff/admin roles, and identity-provider integration | Adds actors, authentication flows, authorization rules, and identity lifecycle behavior. Security NFRs can constrain such a feature but do not themselves require adding it. |
| Email, SMS, push, or other waitlist/enrollment notifications | Adds user-visible behavior, delivery preferences, templates, consent, retry semantics, and failure outcomes. FUT-NFR-12 applies only if that capability is separately approved. |
| New external prerequisite, student-information, payment, or catalog integrations | Adds data ownership, contracts, availability dependencies, and business workflows. |
| Waitlist configuration or policy controls | Adds configurable business behavior, contrary to the current assignment's prohibition on a configurable policy engine. |

Production NFRs do not authorize any of these capabilities by implication.

## 4. Phased roadmap

The phases are a suggested dependency-aware sequence. They do not authorize implementation or change approved requirements.

### Phase 1 — Production foundations

- Confirm product scope, expected workload, data classification, applicable compliance obligations, and business-approved SLOs.
- Select durable persistence and define transaction/uniqueness constraints for capacity, duplicate prevention, FIFO ordering, and promotion.
- Establish authentication and authorization requirements before exposing a production interface.
- Add structured logs, core metrics, health checks, CI security/build gates, and operational runbooks.
- Define backup, restore, retention, and audit needs; test restore and transaction recovery.

### Phase 2 — Scalability and operational resilience

- Load-test the approved workload and tune persistence, indexing, transaction isolation, and application capacity.
- Deploy redundant service instances and a highly available shared data layer where justified.
- Add multi-instance concurrency tests, failover exercises, alerting, SLO dashboards, and regular disaster-recovery drills.
- Introduce controlled deployment, migration, and rollback practices.
- Review scale and reliability objectives against measured workload and operating cost.

### Phase 3 — Advanced capabilities

- Consider separately approved capabilities such as multiple offerings/terms, external integrations, or notifications; define their functional requirements first.
- If an event-driven integration is approved, implement durable event publication, idempotency, retry/dead-letter handling, and contract/version management.
- If a graphical interface is approved, establish accessibility acceptance criteria and conformance testing.
- Consider multi-region recovery, advanced audit controls, or higher SLOs only where business and risk analysis justify the added cost and complexity.

## 5. Approval and verification notes

1. Treat every target, technology, priority, and verification recommendation here as a proposal until approved by product, engineering, operations, and relevant security/privacy stakeholders.
2. Derive final numerical targets from representative workload forecasts, service boundaries, load/failure testing, budget, and business impact analysis.
3. Keep the current assignment's requirements and finalized ADRs unchanged. Any adopted proposal that changes scope should be captured through the project's normal requirements and architecture decision process.
