# Manual Verification Report — Course Registration and Waitlist Management

## Verification evidence

This report records command-prompt output supplied by the user on 2026-11-03. The transcript is user-reported evidence; it was not independently captured by the assistant. The Maven test counts and CLI responses below are transcribed from the supplied output. The supplied Maven excerpt does not include the final `BUILD SUCCESS` or JaCoCo summary lines, so this report does not infer those from the transcript.

### Build and automated tests

**Command**

```text
PS D:\CodeBase\Course_waitlist> mvn clean verify
```

**Observed Surefire output**

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.example.coursewaitlist.application.DemoPrerequisiteValidatorTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.074 s -- in com.example.coursewaitlist.application.DemoPrerequisiteValidatorTest
[INFO] Running com.example.coursewaitlist.application.EnrollmentServiceTest
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.102 s -- in com.example.coursewaitlist.application.EnrollmentServiceTest
[INFO] Running com.example.coursewaitlist.cli.RegistrationCliTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.059 s -- in com.example.coursewaitlist.cli.RegistrationCliTest
[INFO] Running com.example.coursewaitlist.domain.DomainModelsTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.018 s -- in com.example.coursewaitlist.domain.DomainModelsTest
[INFO] Running com.example.coursewaitlist.persistence.InMemoryRegistrationRepositoryTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.044 s -- in com.example.coursewaitlist.persistence.InMemoryRegistrationRepositoryTest
[INFO] Running com.example.coursewaitlist.ProjectSetupTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.example.coursewaitlist.ProjectSetupTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
```

**Assessment:** **PASS (user-provided evidence)** — the supplied output reports 64 tests, 0 failures, 0 errors, and 0 skipped across six test classes. Build completion and coverage are not assessed by this excerpt.

### Focused enrollment service tests

**Command**

```text
PS D:\CodeBase\Course_waitlist> mvn "-Dtest=EnrollmentServiceTest" test
```

**Observed Surefire output**

```text
[INFO] Scanning for projects...
[INFO]
[INFO] --------------< com.example:course-registration-waitlist >--------------
[INFO] Building course-registration-waitlist 1.0-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- jacoco:0.8.13:prepare-agent (default) @ course-registration-waitlist ---
[INFO] argLine set to -javaagent:C:\\Users\\Hp\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.13\\org.jacoco.agent-0.8.13-runtime.jar=destfile=D:\\CodeBase\\Course_waitlist\\target\\jacoco.exec
[INFO]
[INFO] --- resources:3.4.0:resources (default-resources) @ course-registration-waitlist ---
[INFO] --- compiler:3.14.0:compile (default-compile) @ course-registration-waitlist ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] --- resources:3.4.0:testResources (default-testResources) @ course-registration-waitlist ---
[INFO] --- compiler:3.14.0:testCompile (default-testCompile) @ course-registration-waitlist ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] --- surefire:3.5.4:test (default-test) @ course-registration-waitlist ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO]
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.example.coursewaitlist.application.EnrollmentServiceTest
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.160 s -- in com.example.coursewaitlist.application.EnrollmentServiceTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

**Assessment:** **PASS (user-provided evidence)** — all 27 tests in `EnrollmentServiceTest` passed, with 0 failures, errors, or skips. This focused test run is a subset of the full 64-test suite and is not counted as additional unique tests. The command ran Maven's `test` phase; the shown JaCoCo output is agent setup and does not establish a new coverage report or coverage percentage.

### Focused CLI tests

**Command**

```text
PS D:\CodeBase\Course_waitlist> mvn "-Dtest=RegistrationCliTest" test
```

**Observed Surefire output**

```text
[INFO] Scanning for projects...
[INFO]
[INFO] --------------< com.example:course-registration-waitlist >--------------
[INFO] Building course-registration-waitlist 1.0-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- jacoco:0.8.13:prepare-agent (default) @ course-registration-waitlist ---
[INFO] argLine set to -javaagent:C:\\Users\\Hp\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.13\\org.jacoco.agent-0.8.13-runtime.jar=destfile=D:\\CodeBase\\Course_waitlist\\target\\jacoco.exec
[INFO]
[INFO] --- resources:3.4.0:resources (default-resources) @ course-registration-waitlist ---
[INFO] --- compiler:3.14.0:compile (default-compile) @ course-registration-waitlist ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] --- resources:3.4.0:testResources (default-testResources) @ course-registration-waitlist ---
[INFO] --- compiler:3.14.0:testCompile (default-testCompile) @ course-registration-waitlist ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] --- surefire:3.5.4:test (default-test) @ course-registration-waitlist ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO]
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.example.coursewaitlist.cli.RegistrationCliTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.129 s -- in com.example.coursewaitlist.cli.RegistrationCliTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.054 s
```

**Assessment:** **PASS (user-provided evidence)** — all 13 tests in `RegistrationCliTest` passed, with 0 failures, errors, or skips; Maven reported `BUILD SUCCESS`. This focused test run is included in the 64-test full-suite result and is not counted as additional unique tests. JaCoCo agent setup is shown, but this test-phase run does not establish a fresh coverage report or coverage percentage.

### Focused in-memory repository tests

**Command**

```text
PS D:\CodeBase\Course_waitlist> mvn "-Dtest=InMemoryRegistrationRepositoryTest" test
```

**Observed Surefire output**

```text
[INFO] Scanning for projects...
[INFO]
[INFO] --------------< com.example:course-registration-waitlist >--------------
[INFO] Building course-registration-waitlist 1.0-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- jacoco:0.8.13:prepare-agent (default) @ course-registration-waitlist ---
[INFO] argLine set to -javaagent:C:\\Users\\Hp\\.m2\\repository\\org\\jacoco\\org.jacoco.agent\\0.8.13\\org.jacoco.agent-0.8.13-runtime.jar=destfile=D:\\CodeBase\\Course_waitlist\\target\\jacoco.exec
[INFO]
[INFO] --- resources:3.4.0:resources (default-resources) @ course-registration-waitlist ---
[INFO] skip non existing resourceDirectory D:\\CodeBase\\Course_waitlist\\src\\main\\resources
[INFO] --- compiler:3.14.0:compile (default-compile) @ course-registration-waitlist ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] --- resources:3.4.0:testResources (default-testResources) @ course-registration-waitlist ---
[INFO] skip non existing resourceDirectory D:\\CodeBase\\Course_waitlist\\src\\test\\resources
[INFO] --- compiler:3.14.0:testCompile (default-testCompile) @ course-registration-waitlist ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] --- surefire:3.5.4:test (default-test) @ course-registration-waitlist ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO]
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.example.coursewaitlist.persistence.InMemoryRegistrationRepositoryTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.127 s -- in com.example.coursewaitlist.persistence.InMemoryRegistrationRepositoryTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

**Assessment:** **PASS (user-provided evidence)** — all 13 tests in `InMemoryRegistrationRepositoryTest` passed, with 0 failures, errors, or skips; Maven reported `BUILD SUCCESS`. This focused run is already included in the full-suite count and is not counted as additional unique tests. The JaCoCo agent setup does not establish a fresh coverage report or coverage percentage.

### Command-prompt CLI session

**Command**

```text
PS D:\CodeBase\Course_waitlist> java -jar target\course-registration-waitlist-1.0-SNAPSHOT.jar
```

**Observed interactive transcript**

```text
Course Registration CLI. Type HELP for commands.
Help
Commands:
  ENROLL <learner-id>  Request enrollment
  DROP <learner-id>    Drop an enrollment
  VIEW                 Display enrollment and FIFO waitlist state
  HELP                 Show available commands
  EXIT                 Leave the application
ENROLL
INVALID_REQUEST: A valid learner ID is required.
VIEW
Offering capacity: 2
Enrolled learners (0):
  (none)
Waitlisted learners (FIFO):
  (none)
Available seats: 2
ENROLL eligible-1
ENROLLED: Enrollment confirmed.
View
Offering capacity: 2
Enrolled learners (1):
  eligible-1
Waitlisted learners (FIFO):
  (none)
Available seats: 1
ENROLL eligible-2
ENROLLED: Enrollment confirmed.
ENROLL eligible-3
WAITLISTED: Added to the FIFO waitlist.
VIEW
Offering capacity: 2
Enrolled learners (2):
  eligible-1
  eligible-2
Waitlisted learners (FIFO):
  0: eligible-3
Available seats: 0
ENROLL eligible-1
DUPLICATE: Learner is already enrolled.
ENROLL eligible-1
DUPLICATE: Learner is already enrolled.
ENROLL eligible-3
DUPLICATE: Learner is already on the waitlist.
DROP eligible-1
DROPPED: Enrollment removed.
VIEW
Offering capacity: 2
Enrolled learners (2):
  eligible-2
  eligible-3
Waitlisted learners (FIFO):
  (none)
Available seats: 0
DROP eligible-1
NOT_ENROLLED: No active enrollment to drop.
ENROLL unknown-learner
INELIGIBLE: Prerequisite requirements not met.
UNKNOWN
Unknown command. Type HELP for commands.
ENROLL
INVALID_REQUEST: A valid learner ID is required.
EXIT
Goodbye.
```

**Assessment:** **PASS (user-provided interactive evidence)** — the transcript demonstrates startup, help, empty and populated state views, available capacity, enrollment, FIFO waitlisting, duplicate outcomes, drop-triggered promotion, repeated drop, ineligible learner handling, unknown command handling, missing arguments, and clean exit.

## Scenario summary

| Scenario | User-provided evidence | Result |
|---|---|---|
| Maven verification test execution | `mvn clean verify`; 64 tests, 0 failures/errors/skips | **PASS** for the reported test counts |
| Focused enrollment service test execution | `mvn "-Dtest=EnrollmentServiceTest" test`; 27 tests, 0 failures/errors/skips; `BUILD SUCCESS` | **PASS** |
| Focused CLI test execution | `mvn "-Dtest=RegistrationCliTest" test`; 13 tests, 0 failures/errors/skips; `BUILD SUCCESS` | **PASS** |
| Focused in-memory repository test execution | `mvn "-Dtest=InMemoryRegistrationRepositoryTest" test`; 13 tests, 0 failures/errors/skips; `BUILD SUCCESS` | **PASS** |
| Startup and HELP | CLI starts and lists all five supported commands | **PASS** |
| Missing enrollment argument | `ENROLL` returns `INVALID_REQUEST` | **PASS** |
| Empty registration view | Capacity 2, no registrations, 2 available seats | **PASS** |
| Successful enrollment | `eligible-1` and `eligible-2` each return `ENROLLED` | **PASS** |
| Full-course waitlist | `eligible-3` returns `WAITLISTED`; view shows position 0 and no available seats | **PASS** |
| Duplicate enrolled learner | Repeated `ENROLL eligible-1` returns the enrolled duplicate message | **PASS** |
| Duplicate waitlisted learner | `ENROLL eligible-3` again returns the waitlisted duplicate message | **PASS** |
| Drop and promotion | Dropping `eligible-1` leaves `eligible-2` and promotes `eligible-3`; queue is empty | **PASS** |
| Repeated drop | Second drop of `eligible-1` returns `NOT_ENROLLED` | **PASS** |
| Ineligible learner | `ENROLL unknown-learner` returns `INELIGIBLE` | **PASS** |
| Unknown command | `UNKNOWN` receives a help hint | **PASS** |
| Clean exit | `EXIT` returns `Goodbye.` | **PASS** |

This manual evidence does not replace the independently executed Maven/JaCoCo evidence in [VERIFICATION-REPORT.md](./VERIFICATION-REPORT.md). It supplements the earlier scripted CLI checks with the user's reported interactive console run. The AI-use record requirement (AC-12) is unaffected by this transcript.
