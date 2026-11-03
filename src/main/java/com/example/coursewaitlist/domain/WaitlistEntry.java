package com.example.coursewaitlist.domain;

import java.util.Objects;

public record WaitlistEntry(WaitlistEntryId id, LearnerId learnerId, long position) {
    public WaitlistEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(learnerId, "learnerId");
        if (position < 0) {
            throw new IllegalArgumentException("Waitlist position must not be negative");
        }
    }
}
