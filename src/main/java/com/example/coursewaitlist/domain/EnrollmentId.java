package com.example.coursewaitlist.domain;

public record EnrollmentId(long value) {
    public EnrollmentId {
        if (value <= 0) {
            throw new IllegalArgumentException("Enrollment ID must be positive");
        }
    }
}
