package com.example.coursewaitlist.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.coursewaitlist.domain.ActiveRegistration;
import com.example.coursewaitlist.domain.LearnerId;
import com.example.coursewaitlist.domain.Offering;
import com.example.coursewaitlist.domain.RegistrationKind;
import com.example.coursewaitlist.domain.WaitlistEntryId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class InMemoryRegistrationRepositoryTest {
    @Test
    void testTP07AllowsZeroCapacityAndStartsEmpty() {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(0));

        assertEquals(0, repository.snapshot().offering().capacity());
        assertEquals(0, repository.snapshot().enrolledCount());
        assertTrue(repository.snapshot().waitlist().isEmpty());
        assertThrows(
                IllegalStateException.class,
                () -> repository.transact(tx -> tx.saveEnrollment(id("eligible-1"))));
        assertEquals(0, repository.snapshot().enrolledCount());
    }

    @Test
    void testTP01AndTP03MaintainsEnrollmentAndActiveIndex() {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(2));
        LearnerId learnerId = id("eligible-1");

        var enrollment = repository.transact(tx -> tx.saveEnrollment(learnerId));
        RegistrationSnapshot snapshot = repository.snapshot();

        assertEquals(1, snapshot.enrolledCount());
        assertEquals(enrollment, snapshot.enrollments().getFirst());
        assertEquals(
                new ActiveRegistration(learnerId, RegistrationKind.ENROLLED, enrollment.id().value()),
                repository.transact(tx -> tx.findActive(learnerId).orElseThrow()));
        assertThrows(
                IllegalStateException.class,
                () -> repository.transact(tx -> tx.saveEnrollment(learnerId)));
        assertEquals(snapshot, repository.snapshot());
    }

    @Test
    void testTP02AndTP04AppendsWaitlistEntriesInFifoOrderAndRejectsDuplicate() {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(1));
        LearnerId firstLearner = id("eligible-1");
        LearnerId secondLearner = id("eligible-2");

        repository.transact(tx -> tx.saveEnrollment(id("enrolled-1")));
        var first = repository.transact(tx -> tx.saveWaitlistEntry(firstLearner));
        var second = repository.transact(tx -> tx.saveWaitlistEntry(secondLearner));

        assertTrue(first.position() < second.position());
        assertEquals(List.of(first, second), repository.snapshot().waitlist());
        assertEquals(
                new ActiveRegistration(secondLearner, RegistrationKind.WAITLISTED, second.id().value()),
                repository.transact(tx -> tx.findActive(secondLearner).orElseThrow()));
        assertThrows(
                IllegalStateException.class,
                () -> repository.transact(tx -> tx.saveWaitlistEntry(firstLearner)));
        assertEquals(List.of(first, second), repository.snapshot().waitlist());
    }

    @Test
    void testTP08AndTP10RejectsNullConfigurationAndMutations() {
        assertThrows(
                NullPointerException.class,
                () -> new InMemoryRegistrationRepository(null));

        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(1));
        assertThrows(NullPointerException.class, () -> repository.transact(null));
        assertThrows(
                NullPointerException.class,
                () -> repository.transact(tx -> tx.findActive(null)));
        assertThrows(
                NullPointerException.class,
                () -> repository.transact(tx -> tx.saveEnrollment(null)));
        assertThrows(
                NullPointerException.class,
                () -> repository.transact(tx -> tx.saveWaitlistEntry(null)));
        assertThrows(
                NullPointerException.class,
                () -> {
                    repository.transact(tx -> tx.removeEnrollment(null));
                });
        assertThrows(
                NullPointerException.class,
                () -> {
                    repository.transact(tx -> tx.removeWaitlistEntry(null));
                });
        assertEquals(0, repository.snapshot().enrolledCount());
    }

    @Test
    void testTP11AndTP12TransactionAllowsRemovalAndReportsMissingRecords() {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(2));
        var enrollment = repository.transact(tx -> tx.saveEnrollment(id("learner-1")));
        var entry = repository.transact(tx -> tx.saveWaitlistEntry(id("learner-2")));

        assertEquals(Boolean.TRUE, repository.transact(tx -> tx.removeEnrollment(enrollment.id())));
        assertEquals(Boolean.FALSE, repository.transact(tx -> tx.removeEnrollment(enrollment.id())));
        assertEquals(Boolean.TRUE, repository.transact(tx -> tx.removeWaitlistEntry(entry.id())));
        assertEquals(Boolean.FALSE, repository.transact(tx -> tx.removeWaitlistEntry(entry.id())));
        assertTrue(repository.snapshot().enrollments().isEmpty());
        assertTrue(repository.snapshot().waitlist().isEmpty());
    }

    @Test
    void testTP14WaitlistPositionsRemainIncreasingAfterRemovalAndAppend() {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(0));
        var first = repository.transact(tx -> tx.saveWaitlistEntry(id("learner-1")));
        var second = repository.transact(tx -> tx.saveWaitlistEntry(id("learner-2")));

        repository.transact(tx -> tx.removeWaitlistEntry(first.id()));
        var third = repository.transact(tx -> tx.saveWaitlistEntry(id("learner-3")));

        assertEquals(List.of(second, third), repository.snapshot().waitlist());
        assertTrue(second.position() < third.position());
    }

    @Test
    void testTP23AndTP24RollsBackAWholeTransactionAfterFailure() {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(2));
        repository.transact(
                tx -> {
                    tx.saveEnrollment(id("enrolled-1"));
                    tx.saveWaitlistEntry(id("waiting-1"));
                    return null;
                });
        RegistrationSnapshot before = repository.snapshot();

        assertThrows(
                IllegalStateException.class,
                () ->
                        repository.transact(
                                tx -> {
                                    tx.removeEnrollment(before.enrollments().getFirst().id());
                                    tx.removeWaitlistEntry(before.waitlist().getFirst().id());
                                    throw new IllegalStateException("injected failure");
                                }));

        assertEquals(before, repository.snapshot());
    }

    @Test
    void testTP23TransactionViewCannotBeUsedAfterTheAtomicCallbackCompletes() {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(1));
        AtomicReference<RegistrationRepository.Transaction> captured = new AtomicReference<>();

        repository.transact(
                tx -> {
                    captured.set(tx);
                    return null;
                });

        assertNotNull(captured.get());
        assertThrows(IllegalStateException.class, () -> captured.get().countEnrolled());
    }

    @Test
    void testTP23TransactionViewIsThreadBoundAndRejectsNestedTransactions() throws Exception {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(1));
        repository.transact(
                tx -> {
                    AtomicReference<RuntimeException> failure = new AtomicReference<>();
                    Thread otherThread =
                            new Thread(
                                    () -> {
                                        try {
                                            tx.countEnrolled();
                                        } catch (RuntimeException exception) {
                                            failure.set(exception);
                                        }
                                    });
                    otherThread.start();
                    try {
                        otherThread.join(5000);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                    assertFalse(otherThread.isAlive());
                    assertTrue(failure.get() instanceof IllegalStateException);
                    assertThrows(
                            IllegalStateException.class,
                            () -> repository.transact(inner -> inner.countEnrolled()));
                    assertThrows(IllegalStateException.class, repository::snapshot);
                    return null;
                });

        assertEquals(0, repository.snapshot().enrolledCount());
        repository.transact(tx -> tx.saveEnrollment(id("learner-1")));
        assertEquals(1, repository.snapshot().enrolledCount());
    }

    @Test
    void testTP31AndTP32SerializesConcurrentEnrollmentWrites() throws Exception {
        int capacity = 4;
        int requestCount = 12;
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(capacity));
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                int learnerNumber = index;
                results.add(
                        executor.submit(
                                () -> {
                                    ready.countDown();
                                    if (!start.await(5, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("Start signal timed out");
                                    }
                                    try {
                                        repository.transact(
                                                tx ->
                                                        tx.saveEnrollment(
                                                                id("learner-" + learnerNumber)));
                                        return true;
                                    } catch (IllegalStateException noSeat) {
                                        return false;
                                    }
                                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successfulWrites = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    successfulWrites++;
                }
            }

            RegistrationSnapshot snapshot = repository.snapshot();
            assertEquals(capacity, successfulWrites);
            assertEquals(capacity, snapshot.enrolledCount());
            assertEquals(capacity, snapshot.enrollments().stream().map(e -> e.learnerId()).distinct().count());
            assertTrue(snapshot.waitlist().isEmpty());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void simultaneousAttemptsCannotCreateDuplicateActiveRegistrations() throws Exception {
        int requestCount = 8;
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(1));
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        LearnerId learnerId = id("same-learner");

        try {
            for (int index = 0; index < requestCount; index++) {
                results.add(
                        executor.submit(
                                () -> {
                                    ready.countDown();
                                    if (!start.await(5, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("Start signal timed out");
                                    }
                                    try {
                                        repository.transact(tx -> tx.saveEnrollment(learnerId));
                                        return true;
                                    } catch (IllegalStateException duplicate) {
                                        return false;
                                    }
                                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successfulWrites = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    successfulWrites++;
                }
            }

            assertEquals(1, successfulWrites);
            assertEquals(1, repository.snapshot().enrolledCount());
            assertEquals(learnerId, repository.snapshot().enrollments().getFirst().learnerId());
            assertTrue(repository.snapshot().waitlist().isEmpty());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void testAC09TransactionCanReadStateWithoutExposingMutableCollections() {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(2));
        var learnerId = id("learner-1");
        var entry = repository.transact(tx -> tx.saveWaitlistEntry(learnerId));

        repository.transact(
                tx -> {
                    assertEquals(2, tx.getOffering().capacity());
                    assertEquals(0, tx.countEnrolled());
                    assertEquals(List.of(entry), tx.waitlist());
                    assertThrows(UnsupportedOperationException.class, () -> tx.waitlist().clear());
                    assertThrows(UnsupportedOperationException.class, () -> tx.enrollments().clear());
                    return null;
                });
    }

    @Test
    void testTP03AndTP04RejectsEnrollmentAndWaitlistCrossStateDuplicates() {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(1));
        LearnerId learnerId = id("learner-1");

        repository.transact(tx -> tx.saveEnrollment(learnerId));
        RegistrationSnapshot enrolledState = repository.snapshot();
        assertThrows(
                IllegalStateException.class,
                () -> repository.transact(tx -> tx.saveWaitlistEntry(learnerId)));
        assertEquals(enrolledState, repository.snapshot());

        repository.transact(tx -> tx.removeEnrollment(enrolledState.enrollments().getFirst().id()));
        var entry = repository.transact(tx -> tx.saveWaitlistEntry(learnerId));
        RegistrationSnapshot waitlistedState = repository.snapshot();
        assertThrows(
                IllegalStateException.class,
                () -> repository.transact(tx -> tx.saveEnrollment(learnerId)));
        assertEquals(waitlistedState, repository.snapshot());
        assertEquals(entry, repository.snapshot().waitlist().getFirst());
    }

    private static LearnerId id(String value) {
        return new LearnerId(value);
    }
}
