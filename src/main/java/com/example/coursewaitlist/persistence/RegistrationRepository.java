package com.example.coursewaitlist.persistence;

import com.example.coursewaitlist.domain.ActiveRegistration;
import com.example.coursewaitlist.domain.Enrollment;
import com.example.coursewaitlist.domain.EnrollmentId;
import com.example.coursewaitlist.domain.LearnerId;
import com.example.coursewaitlist.domain.Offering;
import com.example.coursewaitlist.domain.WaitlistEntry;
import com.example.coursewaitlist.domain.WaitlistEntryId;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface RegistrationRepository {
    RegistrationSnapshot snapshot();

    <T> T transact(Function<Transaction, T> operation);

    interface Transaction {
        Offering getOffering();

        Optional<ActiveRegistration> findActive(LearnerId learnerId);

        int countEnrolled();

        List<Enrollment> enrollments();

        List<WaitlistEntry> waitlist();

        Enrollment saveEnrollment(LearnerId learnerId);

        boolean removeEnrollment(EnrollmentId enrollmentId);

        WaitlistEntry saveWaitlistEntry(LearnerId learnerId);

        boolean removeWaitlistEntry(WaitlistEntryId entryId);
    }
}
