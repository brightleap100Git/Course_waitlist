package com.example.coursewaitlist.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.coursewaitlist.domain.EligibilityData;
import com.example.coursewaitlist.domain.EligibilityResult;
import com.example.coursewaitlist.domain.LearnerId;
import com.example.coursewaitlist.domain.Offering;
import com.example.coursewaitlist.domain.RegistrationOutcome;
import com.example.coursewaitlist.domain.RegistrationResult;
import com.example.coursewaitlist.domain.WaitlistEntry;
import com.example.coursewaitlist.persistence.InMemoryRegistrationRepository;
import com.example.coursewaitlist.persistence.RegistrationRepository;
import com.example.coursewaitlist.persistence.RegistrationSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class EnrollmentServiceTest {
    @Test
    void testTP01EnrollsEligibleLearnerWhenCapacityRemains() {
        var fixture = fixture(2, Set.of("eligible-1"));

        RegistrationResult result = fixture.service().enroll(id("eligible-1"));

        assertEquals(RegistrationOutcome.ENROLLED, result.outcome());
        assertEquals(1, fixture.repository().snapshot().enrolledCount());
        assertEquals(id("eligible-1"), fixture.repository().snapshot().enrollments().getFirst().learnerId());
        assertTrue(fixture.repository().snapshot().waitlist().isEmpty());
        assertInvariants(fixture.repository());
    }

    @Test
    void testTP02AndTP06WaitlistsEligibleLearnerWhenCourseIsFull() {
        var fixture = fixture(1, Set.of("owner", "eligible-1"));
        seedFullCourse(fixture.repository(), "owner");

        RegistrationResult result = fixture.service().enroll(id("eligible-1"));

        assertEquals(RegistrationOutcome.WAITLISTED, result.outcome());
        assertEquals(List.of(id("eligible-1")), learnerIds(fixture.repository().snapshot().waitlist()));
        assertEquals(1, fixture.repository().snapshot().enrolledCount());
        assertInvariants(fixture.repository());
    }

    @Test
    void testTP03ReturnsAlreadyEnrolledWithoutChangingState() {
        var fixture = fixture(1, Set.of("eligible-1"));
        fixture.service().enroll(id("eligible-1"));
        RegistrationSnapshot before = fixture.repository().snapshot();

        RegistrationResult result = fixture.service().enroll(id("eligible-1"));

        assertEquals(RegistrationOutcome.ALREADY_ENROLLED, result.outcome());
        assertEquals(before, fixture.repository().snapshot());
    }

    @Test
    void testTP04ReturnsAlreadyWaitlistedWithoutChangingQueuePosition() {
        var fixture = fixture(0, Set.of("eligible-1"));
        fixture.service().enroll(id("eligible-1"));
        RegistrationSnapshot before = fixture.repository().snapshot();

        RegistrationResult result = fixture.service().enroll(id("eligible-1"));

        assertEquals(RegistrationOutcome.ALREADY_WAITLISTED, result.outcome());
        assertEquals(before, fixture.repository().snapshot());
    }

    @Test
    void testTP05AndTP36RejectsIneligibleLearnerWithoutMutation() {
        var fixture = fixture(2, Set.of("eligible-1"));
        RegistrationSnapshot before = fixture.repository().snapshot();

        RegistrationResult result = fixture.service().enroll(id("ineligible-1"));

        assertEquals(RegistrationOutcome.INELIGIBLE, result.outcome());
        assertEquals(before, fixture.repository().snapshot());
        assertInvariants(fixture.repository());
    }

    @Test
    void testTP07QueuesEligibleLearnerWhenCapacityIsZero() {
        var fixture = fixture(0, Set.of("eligible-1"));

        RegistrationResult result = fixture.service().enroll(id("eligible-1"));
        RegistrationSnapshot beforeDrop = fixture.repository().snapshot();
        RegistrationResult dropResult = fixture.service().drop(id("eligible-1"));

        assertEquals(RegistrationOutcome.WAITLISTED, result.outcome());
        assertEquals(RegistrationOutcome.NOT_ENROLLED, dropResult.outcome());
        assertEquals(beforeDrop, fixture.repository().snapshot());
        assertEquals(0, fixture.repository().snapshot().enrolledCount());
        assertEquals(List.of(id("eligible-1")), learnerIds(fixture.repository().snapshot().waitlist()));
        assertInvariants(fixture.repository());
    }

    @Test
    void testTP11DropsEnrolledLearnerWhenWaitlistIsEmpty() {
        var fixture = fixture(1, Set.of("eligible-1"));
        fixture.service().enroll(id("eligible-1"));

        RegistrationResult result = fixture.service().drop(id("eligible-1"));

        assertEquals(RegistrationOutcome.DROPPED, result.outcome());
        assertTrue(fixture.repository().snapshot().enrollments().isEmpty());
        assertTrue(fixture.repository().snapshot().waitlist().isEmpty());
        assertInvariants(fixture.repository());
    }

    @Test
    void testTP12AndTP13UnknownOrRepeatedDropReturnsNotEnrolledWithoutMutation() {
        var fixture = fixture(1, Set.of("eligible-1"));
        fixture.service().enroll(id("eligible-1"));

        assertEquals(RegistrationOutcome.DROPPED, fixture.service().drop(id("eligible-1")).outcome());
        RegistrationSnapshot afterSuccessfulDrop = fixture.repository().snapshot();

        assertEquals(RegistrationOutcome.NOT_ENROLLED, fixture.service().drop(id("eligible-1")).outcome());
        assertEquals(RegistrationOutcome.NOT_ENROLLED, fixture.service().drop(id("unknown")).outcome());
        assertEquals(afterSuccessfulDrop, fixture.repository().snapshot());
        assertInvariants(fixture.repository());
    }

    @Test
    void testTP10ReturnsInvalidRequestForNullDropLearnerId() {
        var fixture = fixture(1, Set.of("eligible-1"));
        RegistrationSnapshot before = fixture.repository().snapshot();

        RegistrationResult result = fixture.service().drop(null);

        assertEquals(RegistrationOutcome.INVALID_REQUEST, result.outcome());
        assertEquals(before, fixture.repository().snapshot());
    }

    @Test
    void testTP12DroppingWaitlistedLearnerDoesNotTriggerPromotion() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository(new Offering(1));
        repository.transact(
                tx -> {
                    tx.saveEnrollment(id("owner"));
                    tx.saveWaitlistEntry(id("waitlisted"));
                    return null;
                });
        AtomicInteger eligibilityChecks = new AtomicInteger();
        PrerequisiteValidator validator =
                learnerId -> {
                    eligibilityChecks.incrementAndGet();
                    return new EligibilityResult(true, java.util.Optional.empty());
                };
        EnrollmentService service = service(repository, validator);
        RegistrationSnapshot before = repository.snapshot();

        RegistrationResult result = service.drop(id("waitlisted"));

        assertEquals(RegistrationOutcome.NOT_ENROLLED, result.outcome());
        assertEquals(0, eligibilityChecks.get());
        assertEquals(before, repository.snapshot());
        assertInvariants(repository);
    }

    @Test
    void testTP14PromotesEarliestEligibleWaiterAfterDrop() {
        var fixture = fixture(1, Set.of("owner", "waiter-1", "waiter-2"));
        seedFullCourse(fixture.repository(), "owner");
        fixture.repository()
                .transact(
                        tx -> {
                            tx.saveWaitlistEntry(id("waiter-1"));
                            tx.saveWaitlistEntry(id("waiter-2"));
                            return null;
                        });

        RegistrationResult result = fixture.service().drop(id("owner"));
        RegistrationSnapshot snapshot = fixture.repository().snapshot();

        assertEquals(RegistrationOutcome.DROPPED, result.outcome());
        assertEquals(List.of(id("waiter-1")), snapshot.enrollments().stream().map(e -> e.learnerId()).toList());
        assertEquals(List.of(id("waiter-2")), learnerIds(snapshot.waitlist()));
        assertEquals(1, snapshot.enrolledCount());
        assertInvariants(fixture.repository());
    }

    @Test
    void testTP14AndTP22PromoteWaitlistedLearnersInFifoOrderWhileCapacityRemains() {
        var fixture = fixture(3, Set.of("owner", "owner-2", "waiter-1", "waiter-2", "waiter-3"));
        fixture.repository()
                .transact(
                        tx -> {
                            tx.saveEnrollment(id("owner"));
                            tx.saveEnrollment(id("owner-2"));
                            tx.saveWaitlistEntry(id("waiter-1"));
                            tx.saveWaitlistEntry(id("waiter-2"));
                            tx.saveWaitlistEntry(id("waiter-3"));
                            return null;
                        });

        RegistrationResult result = fixture.service().drop(id("owner"));
        RegistrationSnapshot snapshot = fixture.repository().snapshot();

        assertEquals(RegistrationOutcome.DROPPED, result.outcome());
        assertEquals(
                List.of(id("owner-2"), id("waiter-1"), id("waiter-2")),
                snapshot.enrollments().stream().map(e -> e.learnerId()).toList());
        assertEquals(List.of(id("waiter-3")), learnerIds(snapshot.waitlist()));
        assertEquals(3, snapshot.enrolledCount());
        assertInvariants(fixture.repository());
    }

    @Test
    void testTP15TP17AndTP18RevalidatesAndRetainsIneligibleFrontWaiter() {
        Set<String> currentlyEligible = java.util.concurrent.ConcurrentHashMap.newKeySet();
        currentlyEligible.addAll(Set.of("owner", "owner-2", "waiter-1", "waiter-2"));
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository(new Offering(2));
        repository
                .transact(
                        tx -> {
                            tx.saveEnrollment(id("owner"));
                            tx.saveEnrollment(id("owner-2"));
                            return null;
                        });
        PrerequisiteValidator validator =
                learnerId ->
                        new EligibilityResult(
                                currentlyEligible.contains(learnerId.value()),
                                java.util.Optional.empty());
        EnrollmentService service = service(repository, validator);
        assertEquals(RegistrationOutcome.WAITLISTED, service.enroll(id("waiter-1")).outcome());
        assertEquals(RegistrationOutcome.WAITLISTED, service.enroll(id("waiter-2")).outcome());
        currentlyEligible.remove("waiter-1");
        long skippedPosition = repository.snapshot().waitlist().getFirst().position();

        assertEquals(RegistrationOutcome.DROPPED, service.drop(id("owner")).outcome());
        RegistrationSnapshot snapshot = repository.snapshot();

        assertEquals(
                List.of(id("owner-2"), id("waiter-2")),
                snapshot.enrollments().stream().map(e -> e.learnerId()).toList());
        assertEquals(List.of(id("waiter-1")), learnerIds(snapshot.waitlist()));
        assertEquals(skippedPosition, snapshot.waitlist().getFirst().position());
        assertInvariants(repository);
    }

    @Test
    void testTP16TP17AndTP28LeaveSeatAvailableWhenEveryWaiterFailsRevalidation() {
        Set<String> currentlyEligible = java.util.concurrent.ConcurrentHashMap.newKeySet();
        currentlyEligible.addAll(Set.of("owner", "owner-2", "waiter-1", "waiter-2"));
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository(new Offering(2));
        repository
                .transact(
                        tx -> {
                            tx.saveEnrollment(id("owner"));
                            tx.saveEnrollment(id("owner-2"));
                            return null;
                        });
        PrerequisiteValidator validator =
                learnerId ->
                        new EligibilityResult(
                                currentlyEligible.contains(learnerId.value()),
                                java.util.Optional.empty());
        EnrollmentService service = service(repository, validator);
        assertEquals(RegistrationOutcome.WAITLISTED, service.enroll(id("waiter-1")).outcome());
        assertEquals(RegistrationOutcome.WAITLISTED, service.enroll(id("waiter-2")).outcome());
        currentlyEligible.removeAll(Set.of("waiter-1", "waiter-2"));
        List<Long> positions =
                repository.snapshot().waitlist().stream().map(entry -> entry.position()).toList();

        RegistrationResult result = service.drop(id("owner"));
        RegistrationSnapshot snapshot = repository.snapshot();

        assertEquals(RegistrationOutcome.DROPPED, result.outcome());
        assertEquals(List.of(id("owner-2")), snapshot.enrollments().stream().map(e -> e.learnerId()).toList());
        assertTrue(snapshot.enrolledCount() < snapshot.offering().capacity());
        assertEquals(List.of(id("waiter-1"), id("waiter-2")), learnerIds(snapshot.waitlist()));
        assertEquals(
                positions,
                snapshot.waitlist().stream().map(entry -> entry.position()).toList());
        assertInvariants(repository);
    }

    @Test
    void testTP19RechecksPreviouslyIneligibleWaiterAtLaterAllocationOpportunity() {
        Set<String> currentlyEligible = java.util.concurrent.ConcurrentHashMap.newKeySet();
        currentlyEligible.addAll(Set.of("owner", "newcomer-1", "newcomer-2"));
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository(new Offering(3));
        repository.transact(
                tx -> {
                    tx.saveEnrollment(id("owner"));
                    tx.saveWaitlistEntry(id("waiter"));
                    return null;
                });
        PrerequisiteValidator validator =
                learnerId ->
                        new EligibilityResult(
                                currentlyEligible.contains(learnerId.value()),
                                java.util.Optional.empty());
        EnrollmentService service = service(repository, validator);

        assertEquals(RegistrationOutcome.ENROLLED, service.enroll(id("newcomer-1")).outcome());
        assertEquals(List.of(id("waiter")), learnerIds(repository.snapshot().waitlist()));
        currentlyEligible.add("waiter");

        RegistrationResult result = service.enroll(id("newcomer-2"));
        RegistrationSnapshot snapshot = repository.snapshot();

        assertEquals(RegistrationOutcome.WAITLISTED, result.outcome());
        assertEquals(
                List.of(id("owner"), id("newcomer-1"), id("waiter")),
                snapshot.enrollments().stream().map(e -> e.learnerId()).toList());
        assertEquals(List.of(id("newcomer-2")), learnerIds(snapshot.waitlist()));
        assertInvariants(repository);
    }

    @Test
    void testTP23TP24AndTP26PromotionFailureRollsBackDropAndEarlierPromotion() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository(new Offering(4));
        repository.transact(
                tx -> {
                    tx.saveEnrollment(id("owner"));
                    tx.saveWaitlistEntry(id("waiter-1"));
                    tx.saveWaitlistEntry(id("waiter-2"));
                    return null;
                });
        AtomicInteger checks = new AtomicInteger();
        PrerequisiteValidator validator =
                learnerId -> {
                    if (checks.incrementAndGet() == 2) {
                        throw new IllegalStateException("Injected promotion validation failure");
                    }
                    return new EligibilityResult(true, java.util.Optional.empty());
                };
        EnrollmentService service = service(repository, validator);
        RegistrationSnapshot before = repository.snapshot();

        assertThrows(IllegalStateException.class, () -> service.drop(id("owner")));
        assertEquals(before, repository.snapshot());
        assertInvariants(repository);

        RegistrationResult retry = service.drop(id("owner"));
        RegistrationSnapshot afterRetry = repository.snapshot();
        assertEquals(RegistrationOutcome.DROPPED, retry.outcome());
        assertEquals(
                List.of(id("waiter-1"), id("waiter-2")),
                afterRetry.enrollments().stream().map(e -> e.learnerId()).toList());
        assertTrue(afterRetry.waitlist().isEmpty());
        assertInvariants(repository);
    }

    @Test
    void testTP32ConcurrentDropAndEnrollmentPreserveWaitlistPriorityAndCapacity()
            throws Exception {
        var fixture =
                fixture(
                        2,
                        Set.of("owner-1", "owner-2", "waiter", "newcomer"));
        fixture.repository()
                .transact(
                        tx -> {
                            tx.saveEnrollment(id("owner-1"));
                            tx.saveEnrollment(id("owner-2"));
                            tx.saveWaitlistEntry(id("waiter"));
                            return null;
                        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<RegistrationResult> drop =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                if (!start.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException("Start signal timed out");
                                }
                                return fixture.service().drop(id("owner-1"));
                            });
            Future<RegistrationResult> enroll =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                if (!start.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException("Start signal timed out");
                                }
                                return fixture.service().enroll(id("newcomer"));
                            });

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(RegistrationOutcome.DROPPED, drop.get(5, TimeUnit.SECONDS).outcome());
            assertEquals(RegistrationOutcome.WAITLISTED, enroll.get(5, TimeUnit.SECONDS).outcome());

            RegistrationSnapshot snapshot = fixture.repository().snapshot();
            assertEquals(
                    Set.of(id("owner-2"), id("waiter")),
                    new HashSet<>(snapshot.enrollments().stream().map(e -> e.learnerId()).toList()));
            assertEquals(List.of(id("newcomer")), learnerIds(snapshot.waitlist()));
            assertEquals(2, snapshot.enrolledCount());
            assertInvariants(fixture.repository());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void testTP34ConcurrentDropsPromoteWaitersInFifoOrderWithoutDuplicates() throws Exception {
        var fixture = fixture(2, Set.of("owner-1", "owner-2", "waiter-1", "waiter-2"));
        fixture.repository()
                .transact(
                        tx -> {
                            tx.saveEnrollment(id("owner-1"));
                            tx.saveEnrollment(id("owner-2"));
                            tx.saveWaitlistEntry(id("waiter-1"));
                            tx.saveWaitlistEntry(id("waiter-2"));
                            return null;
                        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<RegistrationResult> firstDrop =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                if (!start.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException("Start signal timed out");
                                }
                                return fixture.service().drop(id("owner-1"));
                            });
            Future<RegistrationResult> secondDrop =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                if (!start.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException("Start signal timed out");
                                }
                                return fixture.service().drop(id("owner-2"));
                            });

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(RegistrationOutcome.DROPPED, firstDrop.get(5, TimeUnit.SECONDS).outcome());
            assertEquals(RegistrationOutcome.DROPPED, secondDrop.get(5, TimeUnit.SECONDS).outcome());

            RegistrationSnapshot snapshot = fixture.repository().snapshot();
            assertEquals(
                    List.of(id("waiter-1"), id("waiter-2")),
                    snapshot.enrollments().stream().map(e -> e.learnerId()).toList());
            assertTrue(snapshot.waitlist().isEmpty());
            assertEquals(2, snapshot.enrolledCount());
            assertInvariants(fixture.repository());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void testTP35ConcurrentRepeatedDropsReleaseOnlyOneSeatAndPromoteOnce() throws Exception {
        var fixture = fixture(1, Set.of("owner", "waiter-1", "waiter-2"));
        fixture.repository()
                .transact(
                        tx -> {
                            tx.saveEnrollment(id("owner"));
                            tx.saveWaitlistEntry(id("waiter-1"));
                            tx.saveWaitlistEntry(id("waiter-2"));
                            return null;
                        });
        int requestCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RegistrationResult>> results = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                results.add(
                        executor.submit(
                                () -> {
                                    ready.countDown();
                                    if (!start.await(5, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("Start signal timed out");
                                    }
                                    return fixture.service().drop(id("owner"));
                                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int dropped = 0;
            int notEnrolled = 0;
            for (Future<RegistrationResult> result : results) {
                RegistrationOutcome outcome = result.get(5, TimeUnit.SECONDS).outcome();
                if (outcome == RegistrationOutcome.DROPPED) {
                    dropped++;
                } else if (outcome == RegistrationOutcome.NOT_ENROLLED) {
                    notEnrolled++;
                }
            }

            RegistrationSnapshot snapshot = fixture.repository().snapshot();
            assertEquals(1, dropped);
            assertEquals(requestCount - 1, notEnrolled);
            assertEquals(List.of(id("waiter-1")), snapshot.enrollments().stream().map(e -> e.learnerId()).toList());
            assertEquals(List.of(id("waiter-2")), learnerIds(snapshot.waitlist()));
            assertInvariants(fixture.repository());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void testTP10ReturnsInvalidRequestForNullLearnerId() {
        var fixture = fixture(1, Set.of("eligible-1"));
        RegistrationSnapshot before = fixture.repository().snapshot();

        RegistrationResult result = fixture.service().enroll(null);

        assertEquals(RegistrationOutcome.INVALID_REQUEST, result.outcome());
        assertEquals(before, fixture.repository().snapshot());
    }

    @Test
    void testLLDAC01AllocatesOpenSeatToExistingEligibleWaiterBeforeNewcomer() {
        var fixture = fixture(2, Set.of("owner", "waiting-1", "newcomer"));
        seedOpenSeatAndWaiter(fixture.repository(), "owner", "waiting-1");

        RegistrationResult result = fixture.service().enroll(id("newcomer"));
        RegistrationSnapshot snapshot = fixture.repository().snapshot();

        assertEquals(RegistrationOutcome.WAITLISTED, result.outcome());
        assertEquals(2, snapshot.enrolledCount());
        assertEquals(
                Set.of(id("owner"), id("waiting-1")),
                new HashSet<>(snapshot.enrollments().stream().map(e -> e.learnerId()).toList()));
        assertEquals(List.of(id("newcomer")), learnerIds(snapshot.waitlist()));
        assertInvariants(fixture.repository());
    }

    @Test
    void testLLDAC02SkipsAndRetainsIneligibleWaiterBeforePromotingNextEligible() {
        var fixture = fixture(2, Set.of("owner", "eligible-waiter", "newcomer"));
        seedOpenSeatAndWaiters(fixture.repository(), "owner", "ineligible-waiter", "eligible-waiter");
        long skippedPosition = fixture.repository().snapshot().waitlist().getFirst().position();

        RegistrationResult result = fixture.service().enroll(id("newcomer"));
        RegistrationSnapshot snapshot = fixture.repository().snapshot();

        assertEquals(RegistrationOutcome.WAITLISTED, result.outcome());
        assertEquals(
                Set.of(id("owner"), id("eligible-waiter")),
                new HashSet<>(snapshot.enrollments().stream().map(e -> e.learnerId()).toList()));
        assertEquals(
                List.of(id("ineligible-waiter"), id("newcomer")),
                learnerIds(snapshot.waitlist()));
        assertEquals(skippedPosition, snapshot.waitlist().getFirst().position());
        assertInvariants(fixture.repository());
    }

    @Test
    void testLLDAC03EnrollsNewcomerWhenAllExistingWaitersAreIneligible() {
        var fixture = fixture(2, Set.of("owner", "newcomer"));
        seedOpenSeatAndWaiters(fixture.repository(), "owner", "ineligible-1", "ineligible-2");
        List<Long> oldPositions =
                fixture.repository().snapshot().waitlist().stream().map(entry -> entry.position()).toList();

        RegistrationResult result = fixture.service().enroll(id("newcomer"));
        RegistrationSnapshot snapshot = fixture.repository().snapshot();

        assertEquals(RegistrationOutcome.ENROLLED, result.outcome());
        assertEquals(
                Set.of(id("owner"), id("newcomer")),
                new HashSet<>(snapshot.enrollments().stream().map(e -> e.learnerId()).toList()));
        assertEquals(List.of(id("ineligible-1"), id("ineligible-2")), learnerIds(snapshot.waitlist()));
        assertEquals(oldPositions, snapshot.waitlist().stream().map(entry -> entry.position()).toList());
        assertInvariants(fixture.repository());
    }

    @Test
    void testTP22AllocatesMultipleOpenSeatsToEligibleWaitersInFifoOrder() {
        var fixture = fixture(3, Set.of("owner", "waiter-1", "waiter-2", "newcomer"));
        seedOpenSeatAndWaiters(fixture.repository(), "owner", "waiter-1", "waiter-2");

        RegistrationResult result = fixture.service().enroll(id("newcomer"));
        RegistrationSnapshot snapshot = fixture.repository().snapshot();

        assertEquals(RegistrationOutcome.WAITLISTED, result.outcome());
        assertEquals(
                List.of(id("owner"), id("waiter-1"), id("waiter-2")),
                snapshot.enrollments().stream().map(enrollment -> enrollment.learnerId()).toList());
        assertEquals(List.of(id("newcomer")), learnerIds(snapshot.waitlist()));
        assertInvariants(fixture.repository());
    }

    @Test
    void testTP25RollsBackWaitlistAllocationsIfLaterEligibilityCheckFails() {
        Set<String> eligible = Set.of("owner-1", "owner-2", "owner-3", "waiter-1", "waiter-2", "newcomer");
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository(new Offering(3));
        repository.transact(
                tx -> {
                    tx.saveEnrollment(id("owner-1"));
                    tx.saveEnrollment(id("owner-2"));
                    tx.saveEnrollment(id("owner-3"));
                    tx.saveWaitlistEntry(id("waiter-1"));
                    tx.saveWaitlistEntry(id("waiter-2"));
                    tx.removeEnrollment(tx.enrollments().get(1).id());
                    tx.removeEnrollment(tx.enrollments().get(1).id());
                    return null;
                });
        RegistrationSnapshot before = repository.snapshot();
        AtomicInteger checks = new AtomicInteger();
        PrerequisiteValidator validator =
                learnerId -> {
                    if (checks.incrementAndGet() == 3) {
                        throw new IllegalStateException("Injected eligibility-source failure");
                    }
                    return new EligibilityResult(
                            eligible.contains(learnerId.value()), java.util.Optional.empty());
                };
        EnrollmentService service = service(repository, validator);

        assertThrows(IllegalStateException.class, () -> service.enroll(id("newcomer")));

        assertEquals(before, repository.snapshot());
        assertInvariants(repository);
    }

    @Test
    void testTP31AndTP32ConcurrentEnrollmentPreservesCapacityAndUniqueness() throws Exception {
        int capacity = 3;
        int requestCount = 10;
        Set<String> eligible = new HashSet<>();
        for (int index = 0; index < requestCount; index++) {
            eligible.add("learner-" + index);
        }
        var fixture = fixture(capacity, eligible);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RegistrationResult>> results = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                int learnerIndex = index;
                results.add(
                        executor.submit(
                                () -> {
                                    ready.countDown();
                                    if (!start.await(5, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("Start signal timed out");
                                    }
                                    return fixture.service().enroll(id("learner-" + learnerIndex));
                                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            for (Future<RegistrationResult> result : results) {
                assertTrue(
                        List.of(RegistrationOutcome.ENROLLED, RegistrationOutcome.WAITLISTED)
                                .contains(result.get(5, TimeUnit.SECONDS).outcome()));
            }

            RegistrationSnapshot snapshot = fixture.repository().snapshot();
            assertEquals(capacity, snapshot.enrolledCount());
            assertEquals(
                    requestCount,
                    snapshot.enrollments().size()
                            + snapshot.waitlist().size());
            assertEquals(
                    requestCount,
                    java.util.stream.Stream.concat(
                                    snapshot.enrollments().stream().map(e -> e.learnerId()),
                                    snapshot.waitlist().stream().map(e -> e.learnerId()))
                            .distinct()
                            .count());
            assertInvariants(fixture.repository());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void rejectsNullDependencies() {
        var fixture = fixture(1, Set.of());
        assertThrows(
                NullPointerException.class,
                () ->
                        new EnrollmentService(
                                null, fixture.validator(), fixture.promotion(), fixture.waitlist()));
        assertThrows(
                NullPointerException.class,
                () ->
                        new EnrollmentService(
                                fixture.repository(), null, fixture.promotion(), fixture.waitlist()));
        assertThrows(
                NullPointerException.class,
                () ->
                        new EnrollmentService(
                                fixture.repository(), fixture.validator(), null, fixture.waitlist()));
        assertThrows(
                NullPointerException.class,
                () ->
                        new EnrollmentService(
                                fixture.repository(), fixture.validator(), fixture.promotion(), null));
        assertThrows(NullPointerException.class, () -> new PromotionCoordinator(null, fixture.waitlist()));
        assertThrows(NullPointerException.class, () -> new PromotionCoordinator(fixture.validator(), null));
    }

    private static Fixture fixture(int capacity, Set<String> eligibleIds) {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(capacity));
        PrerequisiteValidator validator = validator(eligibleIds);
        WaitlistManager waitlist = new WaitlistManager();
        PromotionCoordinator promotion = new PromotionCoordinator(validator, waitlist);
        return new Fixture(
                repository,
                validator,
                promotion,
                waitlist,
                new EnrollmentService(repository, validator, promotion, waitlist));
    }

    private static EnrollmentService service(
            RegistrationRepository repository, PrerequisiteValidator validator) {
        WaitlistManager waitlist = new WaitlistManager();
        PromotionCoordinator promotion = new PromotionCoordinator(validator, waitlist);
        return new EnrollmentService(repository, validator, promotion, waitlist);
    }

    private static PrerequisiteValidator validator(Set<String> eligibleIds) {
        Set<String> immutableIds = Set.copyOf(eligibleIds);
        EligibilityData eligibilityData =
                new EligibilityData(
                        immutableIds.stream()
                                .map(EnrollmentServiceTest::id)
                                .collect(java.util.stream.Collectors.toSet()));
        return new DemoPrerequisiteValidator(eligibilityData);
    }

    private static void seedFullCourse(InMemoryRegistrationRepository repository, String owner) {
        repository.transact(tx -> tx.saveEnrollment(id(owner)));
    }

    private static void seedOpenSeatAndWaiter(
            InMemoryRegistrationRepository repository, String owner, String waiter) {
        seedOpenSeatAndWaiters(repository, owner, waiter);
    }

    private static void seedOpenSeatAndWaiters(
            InMemoryRegistrationRepository repository, String owner, String... waiters) {
        repository.transact(
                tx -> {
                    tx.saveEnrollment(id(owner));
                    for (String waiter : waiters) {
                        tx.saveWaitlistEntry(id(waiter));
                    }
                    return null;
                });
    }

    private static void assertInvariants(InMemoryRegistrationRepository repository) {
        RegistrationSnapshot snapshot = repository.snapshot();
        assertTrue(snapshot.offering().capacity() >= 0);
        assertTrue(snapshot.enrolledCount() <= snapshot.offering().capacity());
        List<LearnerId> enrolled = snapshot.enrollments().stream().map(e -> e.learnerId()).toList();
        List<LearnerId> waitlisted = learnerIds(snapshot.waitlist());
        Set<LearnerId> all = new HashSet<>(enrolled);
        assertEquals(enrolled.size(), all.size());
        assertEquals(waitlisted.size(), new HashSet<>(waitlisted).size());
        waitlisted.forEach(learner -> assertFalse(all.contains(learner)));
        long previousPosition = -1;
        for (WaitlistEntry entry : snapshot.waitlist()) {
            assertTrue(previousPosition < entry.position());
            previousPosition = entry.position();
        }
    }

    private static List<LearnerId> learnerIds(List<WaitlistEntry> entries) {
        return entries.stream().map(entry -> entry.learnerId()).toList();
    }

    private static LearnerId id(String value) {
        return new LearnerId(value);
    }

    private record Fixture(
            InMemoryRegistrationRepository repository,
            PrerequisiteValidator validator,
            PromotionCoordinator promotion,
            WaitlistManager waitlist,
            EnrollmentService service) {}
}
