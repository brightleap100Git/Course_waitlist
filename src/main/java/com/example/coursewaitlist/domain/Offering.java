package com.example.coursewaitlist.domain;

public record Offering(int capacity) {
    public Offering {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity must not be negative");
        }
    }
}
