package com.example.coursewaitlist.domain;

import java.util.Objects;
import java.util.Set;

public record EligibilityData(Set<LearnerId> eligibleLearners) {
    public EligibilityData {
        eligibleLearners = Set.copyOf(Objects.requireNonNull(eligibleLearners, "eligibleLearners"));
    }

    public boolean isEligible(LearnerId learnerId) {
        return eligibleLearners.contains(Objects.requireNonNull(learnerId, "learnerId"));
    }
}
