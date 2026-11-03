package com.example.coursewaitlist.application;

import com.example.coursewaitlist.domain.EligibilityResult;
import com.example.coursewaitlist.domain.WaitlistEntry;
import com.example.coursewaitlist.persistence.RegistrationRepository;
import java.util.Objects;
import java.util.Optional;

public final class PromotionCoordinator {
    private final PrerequisiteValidator prerequisiteValidator;
    private final WaitlistManager waitlistManager;

    public PromotionCoordinator(
            PrerequisiteValidator prerequisiteValidator, WaitlistManager waitlistManager) {
        this.prerequisiteValidator =
                Objects.requireNonNull(prerequisiteValidator, "prerequisiteValidator");
        this.waitlistManager = Objects.requireNonNull(waitlistManager, "waitlistManager");
    }

    public int allocateAvailableSeats(RegistrationRepository.Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction");
        int promotedCount = 0;
        long afterPosition = -1;

        while (transaction.countEnrolled() < transaction.getOffering().capacity()) {
            Optional<WaitlistEntry> next =
                    waitlistManager.nextCandidate(transaction, afterPosition);
            if (next.isEmpty()) {
                break;
            }

            WaitlistEntry candidate = next.orElseThrow();
            afterPosition = candidate.position();
            EligibilityResult eligibility =
                    prerequisiteValidator.check(candidate.learnerId());
            if (!eligibility.eligible()) {
                continue;
            }

            waitlistManager.remove(transaction, candidate.id());
            transaction.saveEnrollment(candidate.learnerId());
            promotedCount++;
        }

        return promotedCount;
    }
}
