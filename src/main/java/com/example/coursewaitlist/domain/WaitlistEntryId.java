package com.example.coursewaitlist.domain;

public record WaitlistEntryId(long value) {
    public WaitlistEntryId {
        if (value <= 0) {
            throw new IllegalArgumentException("Waitlist entry ID must be positive");
        }
    }
}
