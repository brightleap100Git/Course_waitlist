package com.example.coursewaitlist.domain;

import java.util.Objects;
import java.util.Optional;

public record RegistrationResult(RegistrationOutcome outcome, Optional<String> message) {
    public RegistrationResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(message, "message");
    }

    public RegistrationResult(RegistrationOutcome outcome) {
        this(outcome, Optional.empty());
    }
}
