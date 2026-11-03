package com.example.coursewaitlist.application;

import com.example.coursewaitlist.domain.EligibilityResult;
import com.example.coursewaitlist.domain.LearnerId;

public interface PrerequisiteValidator {
    EligibilityResult check(LearnerId learnerId);
}
