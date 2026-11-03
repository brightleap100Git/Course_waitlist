package com.example.coursewaitlist.domain;

import java.util.Objects;

public record LearnerId(String value) {
    public LearnerId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Learner ID must not be blank");
        }
    }
}
