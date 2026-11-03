package com.example.coursewaitlist.domain;

import java.util.Objects;
import java.util.Optional;

public record EligibilityResult(boolean eligible, Optional<String> reason) {
    public EligibilityResult {
        Objects.requireNonNull(reason, "reason");
    }
}
