package com.example.coursewaitlist.application;

import com.example.coursewaitlist.domain.EligibilityData;
import com.example.coursewaitlist.domain.EligibilityResult;
import com.example.coursewaitlist.domain.LearnerId;
import java.util.Objects;
import java.util.Optional;

public final class DemoPrerequisiteValidator implements PrerequisiteValidator {
    private static final String INELIGIBLE_REASON =
            "Learner is not present in the demo eligible-learner set";

    private final EligibilityData eligibilityData;

    public DemoPrerequisiteValidator(EligibilityData eligibilityData) {
        this.eligibilityData = Objects.requireNonNull(eligibilityData, "eligibilityData");
    }

    @Override
    public EligibilityResult check(LearnerId learnerId) {
        boolean eligible = eligibilityData.isEligible(Objects.requireNonNull(learnerId, "learnerId"));
        return new EligibilityResult(
                eligible,
                eligible ? Optional.empty() : Optional.of(INELIGIBLE_REASON));
    }
}
