package com.example.coursewaitlist.persistence;

import com.example.coursewaitlist.domain.Enrollment;
import com.example.coursewaitlist.domain.Offering;
import com.example.coursewaitlist.domain.WaitlistEntry;
import java.util.List;
import java.util.Objects;

public record RegistrationSnapshot(
        Offering offering,
        List<Enrollment> enrollments,
        List<WaitlistEntry> waitlist) {
    public RegistrationSnapshot {
        Objects.requireNonNull(offering, "offering");
        enrollments = List.copyOf(Objects.requireNonNull(enrollments, "enrollments"));
        waitlist = List.copyOf(Objects.requireNonNull(waitlist, "waitlist"));
    }

    public int enrolledCount() {
        return enrollments.size();
    }
}
