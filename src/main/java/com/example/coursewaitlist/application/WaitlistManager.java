package com.example.coursewaitlist.application;

import com.example.coursewaitlist.domain.LearnerId;
import com.example.coursewaitlist.domain.WaitlistEntry;
import com.example.coursewaitlist.domain.WaitlistEntryId;
import com.example.coursewaitlist.persistence.RegistrationRepository;
import java.util.Objects;
import java.util.Optional;

public final class WaitlistManager {
    public WaitlistEntry enqueue(
            RegistrationRepository.Transaction transaction, LearnerId learnerId) {
        return Objects.requireNonNull(transaction, "transaction")
                .saveWaitlistEntry(Objects.requireNonNull(learnerId, "learnerId"));
    }

    public Optional<WaitlistEntry> nextCandidate(
            RegistrationRepository.Transaction transaction, long afterPosition) {
        return Objects.requireNonNull(transaction, "transaction").waitlist().stream()
                .filter(entry -> entry.position() > afterPosition)
                .findFirst();
    }

    public void remove(
            RegistrationRepository.Transaction transaction, WaitlistEntryId entryId) {
        boolean removed =
                Objects.requireNonNull(transaction, "transaction")
                        .removeWaitlistEntry(Objects.requireNonNull(entryId, "entryId"));
        if (!removed) {
            throw new IllegalStateException("Waitlist entry no longer exists");
        }
    }
}
