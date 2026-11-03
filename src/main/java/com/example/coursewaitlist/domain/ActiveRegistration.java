package com.example.coursewaitlist.domain;

import java.util.Objects;

public record ActiveRegistration(LearnerId learnerId, RegistrationKind kind, long recordId) {
    public ActiveRegistration {
        Objects.requireNonNull(learnerId, "learnerId");
        Objects.requireNonNull(kind, "kind");
        if (recordId <= 0) {
            throw new IllegalArgumentException("Record ID must be positive");
        }
    }
}
