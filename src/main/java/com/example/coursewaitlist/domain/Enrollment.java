package com.example.coursewaitlist.domain;

import java.util.Objects;

public record Enrollment(EnrollmentId id, LearnerId learnerId) {
    public Enrollment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(learnerId, "learnerId");
    }
}
