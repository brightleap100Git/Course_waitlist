package com.example.coursewaitlist.persistence;

import com.example.coursewaitlist.domain.ActiveRegistration;
import com.example.coursewaitlist.domain.Enrollment;
import com.example.coursewaitlist.domain.EnrollmentId;
import com.example.coursewaitlist.domain.LearnerId;
import com.example.coursewaitlist.domain.Offering;
import com.example.coursewaitlist.domain.RegistrationKind;
import com.example.coursewaitlist.domain.WaitlistEntry;
import com.example.coursewaitlist.domain.WaitlistEntryId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class InMemoryRegistrationRepository implements RegistrationRepository {
    private State state;
    private boolean transactionActive;

    public InMemoryRegistrationRepository(Offering offering) {
        state = new State(Objects.requireNonNull(offering, "offering"));
    }

    @Override
    public synchronized RegistrationSnapshot snapshot() {
        if (transactionActive) {
            throw new IllegalStateException("Read state through the active transaction");
        }
        return state.snapshot();
    }

    @Override
    public synchronized <T> T transact(Function<Transaction, T> operation) {
        Objects.requireNonNull(operation, "operation");
        if (transactionActive) {
            throw new IllegalStateException("Nested transactions are not supported");
        }
        WorkingState workingState = new WorkingState(state);
        MutableTransaction transaction = new MutableTransaction(workingState);
        transactionActive = true;
        try {
            T result = operation.apply(transaction);
            state = workingState.freeze();
            return result;
        } finally {
            transaction.close();
            transactionActive = false;
        }
    }

    private static final class State {
        private final Offering offering;
        private final LinkedHashMap<EnrollmentId, Enrollment> enrollments;
        private final LinkedHashMap<WaitlistEntryId, WaitlistEntry> waitlist;
        private final Map<LearnerId, ActiveRegistration> activeRegistrations;
        private final long nextEnrollmentId;
        private final long nextWaitlistEntryId;
        private final long nextWaitlistPosition;

        private State(Offering offering) {
            this(offering, new LinkedHashMap<>(), new LinkedHashMap<>(), Map.of(), 1, 1, 0);
        }

        private State(
                Offering offering,
                LinkedHashMap<EnrollmentId, Enrollment> enrollments,
                LinkedHashMap<WaitlistEntryId, WaitlistEntry> waitlist,
                Map<LearnerId, ActiveRegistration> activeRegistrations,
                long nextEnrollmentId,
                long nextWaitlistEntryId,
                long nextWaitlistPosition) {
            this.offering = offering;
            this.enrollments = new LinkedHashMap<>(enrollments);
            this.waitlist = new LinkedHashMap<>(waitlist);
            this.activeRegistrations = Map.copyOf(activeRegistrations);
            this.nextEnrollmentId = nextEnrollmentId;
            this.nextWaitlistEntryId = nextWaitlistEntryId;
            this.nextWaitlistPosition = nextWaitlistPosition;
        }

        private RegistrationSnapshot snapshot() {
            return new RegistrationSnapshot(
                    offering, List.copyOf(enrollments.values()), List.copyOf(waitlist.values()));
        }
    }

    private static final class WorkingState {
        private final Offering offering;
        private final LinkedHashMap<EnrollmentId, Enrollment> enrollments;
        private final LinkedHashMap<WaitlistEntryId, WaitlistEntry> waitlist;
        private final Map<LearnerId, ActiveRegistration> activeRegistrations;
        private long nextEnrollmentId;
        private long nextWaitlistEntryId;
        private long nextWaitlistPosition;

        private WorkingState(State state) {
            offering = state.offering;
            enrollments = new LinkedHashMap<>(state.enrollments);
            waitlist = new LinkedHashMap<>(state.waitlist);
            activeRegistrations = new LinkedHashMap<>(state.activeRegistrations);
            nextEnrollmentId = state.nextEnrollmentId;
            nextWaitlistEntryId = state.nextWaitlistEntryId;
            nextWaitlistPosition = state.nextWaitlistPosition;
        }

        private State freeze() {
            return new State(
                    offering,
                    enrollments,
                    waitlist,
                    activeRegistrations,
                    nextEnrollmentId,
                    nextWaitlistEntryId,
                    nextWaitlistPosition);
        }
    }

    private static final class MutableTransaction implements Transaction {
        private final WorkingState state;
        private final Thread ownerThread;
        private boolean active = true;

        private MutableTransaction(WorkingState state) {
            this.state = state;
            ownerThread = Thread.currentThread();
        }

        @Override
        public Offering getOffering() {
            ensureActive();
            return state.offering;
        }

        @Override
        public Optional<ActiveRegistration> findActive(LearnerId learnerId) {
            ensureActive();
            return Optional.ofNullable(
                    state.activeRegistrations.get(Objects.requireNonNull(learnerId, "learnerId")));
        }

        @Override
        public int countEnrolled() {
            ensureActive();
            return state.enrollments.size();
        }

        @Override
        public List<Enrollment> enrollments() {
            ensureActive();
            return List.copyOf(state.enrollments.values());
        }

        @Override
        public List<WaitlistEntry> waitlist() {
            ensureActive();
            return List.copyOf(state.waitlist.values());
        }

        @Override
        public Enrollment saveEnrollment(LearnerId learnerId) {
            ensureActive();
            Objects.requireNonNull(learnerId, "learnerId");
            requireNotActive(learnerId);
            if (state.enrollments.size() >= state.offering.capacity()) {
                throw new IllegalStateException("Offering has no available seats");
            }
            EnrollmentId id = new EnrollmentId(state.nextEnrollmentId);
            state.nextEnrollmentId = Math.incrementExact(state.nextEnrollmentId);
            Enrollment enrollment = new Enrollment(id, learnerId);
            state.enrollments.put(id, enrollment);
            state.activeRegistrations.put(
                    learnerId,
                    new ActiveRegistration(learnerId, RegistrationKind.ENROLLED, id.value()));
            return enrollment;
        }

        @Override
        public boolean removeEnrollment(EnrollmentId enrollmentId) {
            ensureActive();
            EnrollmentId id = Objects.requireNonNull(enrollmentId, "enrollmentId");
            Enrollment removed = state.enrollments.remove(id);
            if (removed == null) {
                return false;
            }
            state.activeRegistrations.remove(removed.learnerId());
            return true;
        }

        @Override
        public WaitlistEntry saveWaitlistEntry(LearnerId learnerId) {
            ensureActive();
            Objects.requireNonNull(learnerId, "learnerId");
            requireNotActive(learnerId);
            WaitlistEntryId id = new WaitlistEntryId(state.nextWaitlistEntryId);
            state.nextWaitlistEntryId = Math.incrementExact(state.nextWaitlistEntryId);
            long position = state.nextWaitlistPosition;
            state.nextWaitlistPosition = Math.incrementExact(state.nextWaitlistPosition);
            WaitlistEntry entry = new WaitlistEntry(id, learnerId, position);
            state.waitlist.put(id, entry);
            state.activeRegistrations.put(
                    learnerId,
                    new ActiveRegistration(learnerId, RegistrationKind.WAITLISTED, id.value()));
            return entry;
        }

        @Override
        public boolean removeWaitlistEntry(WaitlistEntryId entryId) {
            ensureActive();
            WaitlistEntryId id = Objects.requireNonNull(entryId, "entryId");
            WaitlistEntry removed = state.waitlist.remove(id);
            if (removed == null) {
                return false;
            }
            state.activeRegistrations.remove(removed.learnerId());
            return true;
        }

        private void requireNotActive(LearnerId learnerId) {
            if (state.activeRegistrations.containsKey(learnerId)) {
                throw new IllegalStateException("Learner already has an active registration");
            }
        }

        private void ensureActive() {
            if (!active) {
                throw new IllegalStateException("Transaction is no longer active");
            }
            if (Thread.currentThread() != ownerThread) {
                throw new IllegalStateException("Transaction must be used by its owning thread");
            }
        }

        private void close() {
            active = false;
        }
    }
}
