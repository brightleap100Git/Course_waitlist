package com.example.coursewaitlist.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import com.example.coursewaitlist.persistence.RegistrationSnapshot;
import org.junit.jupiter.api.Test;

class DomainModelsTest {
    @Test
    void testTP07AcceptsZeroAndPositiveCapacity() {
        assertEquals(0, new Offering(0).capacity());
        assertEquals(3, new Offering(3).capacity());
    }

    @Test
    void testTP08RejectsNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new Offering(-1));
    }

    @Test
    void testTP10ValidatesLearnerIds() {
        assertEquals("learner-1", new LearnerId("learner-1").value());
        assertThrows(NullPointerException.class, () -> new LearnerId(null));
        assertThrows(IllegalArgumentException.class, () -> new LearnerId(""));
        assertThrows(IllegalArgumentException.class, () -> new LearnerId("  "));
    }

    @Test
    void testTP03AndTP04ValidatesRegistrationEntitiesAndIdentifiers() {
        LearnerId learnerId = new LearnerId("learner-1");
        EnrollmentId enrollmentId = new EnrollmentId(1);
        WaitlistEntryId waitlistEntryId = new WaitlistEntryId(1);

        assertEquals(learnerId, new Enrollment(enrollmentId, learnerId).learnerId());
        assertEquals(0, new WaitlistEntry(waitlistEntryId, learnerId, 0).position());
        assertEquals(1, new ActiveRegistration(learnerId, RegistrationKind.ENROLLED, 1).recordId());

        assertThrows(IllegalArgumentException.class, () -> new EnrollmentId(0));
        assertThrows(IllegalArgumentException.class, () -> new EnrollmentId(-1));
        assertThrows(IllegalArgumentException.class, () -> new WaitlistEntryId(0));
        assertThrows(IllegalArgumentException.class, () -> new WaitlistEntryId(-1));
        assertThrows(NullPointerException.class, () -> new Enrollment(null, learnerId));
        assertThrows(NullPointerException.class, () -> new Enrollment(enrollmentId, null));
        assertThrows(NullPointerException.class, () -> new WaitlistEntry(null, learnerId, 0));
        assertThrows(NullPointerException.class, () -> new WaitlistEntry(waitlistEntryId, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new WaitlistEntry(waitlistEntryId, learnerId, -1));
        assertThrows(NullPointerException.class, () -> new ActiveRegistration(null, RegistrationKind.ENROLLED, 1));
        assertThrows(NullPointerException.class, () -> new ActiveRegistration(learnerId, null, 1));
        assertThrows(IllegalArgumentException.class, () -> new ActiveRegistration(learnerId, RegistrationKind.ENROLLED, 0));
    }

    @Test
    void testTP05EligibilityDataIsDefensiveAndNullSafe() {
        LearnerId learnerId = new LearnerId("eligible-1");
        Set<LearnerId> mutableIds = new HashSet<>();
        mutableIds.add(learnerId);
        EligibilityData data = new EligibilityData(mutableIds);
        mutableIds.clear();

        assertTrue(data.isEligible(learnerId));
        assertFalse(data.isEligible(new LearnerId("ineligible-1")));
        assertThrows(UnsupportedOperationException.class, () -> data.eligibleLearners().clear());
        assertThrows(NullPointerException.class, () -> data.isEligible(null));
        assertThrows(NullPointerException.class, () -> new EligibilityData(null));
        Set<LearnerId> withNullId = new HashSet<>();
        withNullId.add(null);
        assertThrows(NullPointerException.class, () -> new EligibilityData(withNullId));
    }

    @Test
    void testTP03AndTP05ValidatesEligibilityAndRegistrationResults() {
        assertTrue(new EligibilityResult(true, Optional.empty()).eligible());
        assertFalse(new EligibilityResult(false, Optional.of("prerequisite missing")).eligible());
        assertThrows(NullPointerException.class, () -> new EligibilityResult(true, null));

        assertEquals(
                Optional.empty(),
                new RegistrationResult(RegistrationOutcome.ENROLLED).message());
        assertEquals(
                Optional.of("Queued"),
                new RegistrationResult(RegistrationOutcome.WAITLISTED, Optional.of("Queued"))
                        .message());
        assertThrows(
                NullPointerException.class,
                () -> new RegistrationResult(null, Optional.empty()));
        assertThrows(
                NullPointerException.class,
                () -> new RegistrationResult(RegistrationOutcome.ENROLLED, null));
    }

    @Test
    void testAC09RegistrationSnapshotCopiesCollectionsAndCountsEnrollments() {
        Offering offering = new Offering(1);
        var mutableEnrollments = new java.util.ArrayList<Enrollment>();
        var mutableWaitlist = new java.util.ArrayList<WaitlistEntry>();
        RegistrationSnapshot snapshot =
                new RegistrationSnapshot(offering, mutableEnrollments, mutableWaitlist);

        assertEquals(0, snapshot.enrolledCount());
        assertNotSame(mutableEnrollments, snapshot.enrollments());
        assertNotSame(mutableWaitlist, snapshot.waitlist());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.enrollments().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.waitlist().clear());
        assertThrows(
                NullPointerException.class,
                () -> new RegistrationSnapshot(null, java.util.List.of(), java.util.List.of()));
        assertThrows(
                NullPointerException.class,
                () -> new RegistrationSnapshot(offering, null, java.util.List.of()));
        assertThrows(
                NullPointerException.class,
                () -> new RegistrationSnapshot(offering, java.util.List.of(), null));
    }
}
