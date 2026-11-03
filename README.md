# Course Registration and Waitlist CLI

A Java 21, Maven-based prototype for enrollment, dropping, and FIFO waitlist promotion for one course offering. The application uses a command-line interface and a single-process in-memory repository. It is an assignment-sized service slice, not a production registration platform.

## Scope and behavior

- One course offering; demo capacity is `2`.
- The demo prerequisite fixture treats `eligible-1` through `eligible-4` as eligible. This is test/demo data, not an authoritative prerequisite source.
- Eligible learners enroll when a seat is available, otherwise they join the waitlist.
- Duplicate enrollment and waitlist requests do not create duplicate state.
- On promotion, eligibility is rechecked. An ineligible waiter is skipped for that allocation and retained at their original FIFO position.
- Existing eligible waitlisted learners are considered before a new request receives an available seat.
- Dropping an enrolled learner releases the seat and promotes the earliest eligible waiter. If none is eligible, the seat remains available.
- State is in memory and resets when the process exits.

These policies resolve assignment ambiguities and are design decisions documented in the [ADRs](architecture/architecture-decisions.md); they should not be mistaken for additional requirements in the assignment.

## Prerequisites

- JDK 21
- Apache Maven installed directly and available as `mvn` on `PATH`

Maven Wrapper is not used.

## Build, test, and coverage

Run from the project root:

```powershell
mvn compile
mvn test
mvn clean verify
mvn package
```

`mvn clean verify` runs the JUnit 5 suite, checks configured JaCoCo thresholds, and generates the report at `target\site\jacoco\index.html`. The XML coverage report is `target\site\jacoco\jacoco.xml`; Surefire reports are in `target\surefire-reports\`.

## Run the CLI

```powershell
mvn package
java -jar target\course-registration-waitlist-1.0-SNAPSHOT.jar
```

Commands: `ENROLL <learner-id>`, `DROP <learner-id>`, `VIEW`, `HELP`, and `EXIT`.

### Enrollment, waitlist, and promotion demonstration

Enter the following commands in one CLI session:

```text
HELP
ENROLL eligible-1
ENROLL eligible-2
ENROLL eligible-3
VIEW
DROP eligible-1
VIEW
EXIT
```

The first two learners occupy the offering, the third joins the FIFO waitlist, and dropping `eligible-1` promotes `eligible-3`. `VIEW` displays capacity, enrolled learners, waitlist order, and available seats.

## Project documentation

| Document | Purpose |
|---|---|
| [Requirements](requirement.md) | Assignment-derived functional requirements, constraints, acceptance criteria, and scope. |
| [Implementation plan](implementation-plan.md) | Staged implementation plan and planning-time baseline; its initial project inspection is historical. |
| [Test plan](test-plan.md) | Test scenarios, test-plan IDs, invariants, and planned traceability. |
| [C4 context](architecture/c4-context.md) | Learner and system context. |
| [C4 container](architecture/c4-container.md) | Single-application container view. |
| [C4 components](architecture/c4-component.md) | Logical application responsibilities and interactions. |
| [Low-level design](architecture/lld.md) | Logical models, invariants, flows, and transaction boundaries. |
| [Architecture decisions](architecture/architecture-decisions.md) | Selected waitlist, priority, concurrency, and persistence policies. |
| [Verification report](docs/VERIFICATION-REPORT.md) | Executed test and coverage evidence, traceability, risks, and readiness recommendation. |
| [Manual verification report](docs/MANUAL-VERIFICATION-REPORT.md) | User-provided command-prompt test and CLI evidence, clearly attributed. |
| [AI usage report](docs/AI-USAGE-REPORT.md) | Available AI-use evidence, interaction log, AC-12 status, and historical evidence limitations. |
| [Future improvements](docs/FUTURE-IMPROVEMENTS.md) | Proposed production NFRs and phased roadmap; targets are illustrative, not approved commitments. |

## Verification status and limitations

The verification report records **64 passing tests** and JaCoCo coverage of **99.42% lines / 96.97% branches overall**; business/domain coverage is **98.60% lines / 95.24% branches**. The recommendation remains **CONDITIONAL GO**: the [AI usage report](docs/AI-USAGE-REPORT.md) documents available evidence, but missing historical prompt/response records and suggestion dispositions leave AC-12 only partially demonstrated.

This prototype has no durable persistence, multi-process coordination, human identity integration, production API, deployment, or authoritative prerequisite data. Manual command-prompt evidence was supplied by the user and is labeled accordingly; it is not presented as independently captured testing. Production-oriented proposals are in [Future Improvements](docs/FUTURE-IMPROVEMENTS.md) and do not expand the current assignment scope.
