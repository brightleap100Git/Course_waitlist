package com.example.coursewaitlist.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.coursewaitlist.domain.EligibilityData;
import com.example.coursewaitlist.domain.LearnerId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DemoPrerequisiteValidatorTest {
    @Test
    void testTP05AcceptsIdsInDemoEligibilityFixture() {
        LearnerId eligibleLearner = id("eligible-1");
        DemoPrerequisiteValidator validator =
                new DemoPrerequisiteValidator(new EligibilityData(Set.of(eligibleLearner)));

        var result = validator.check(eligibleLearner);

        assertTrue(result.eligible());
        assertTrue(result.reason().isEmpty());
    }

    @Test
    void testTP05RejectsIdsMissingFromDemoEligibilityFixture() {
        DemoPrerequisiteValidator validator =
                new DemoPrerequisiteValidator(new EligibilityData(Set.of(id("eligible-1"))));

        var result = validator.check(id("ineligible-1"));

        assertFalse(result.eligible());
        assertNotNull(result.reason().orElseThrow());
        assertEquals(
                "Learner is not present in the demo eligible-learner set",
                result.reason().orElseThrow());
    }

    @Test
    void rejectsNullFixtureAndLearnerId() {
        assertThrows(NullPointerException.class, () -> new DemoPrerequisiteValidator(null));
        DemoPrerequisiteValidator validator =
                new DemoPrerequisiteValidator(new EligibilityData(Set.of()));

        assertThrows(NullPointerException.class, () -> validator.check(null));
    }

    private static LearnerId id(String value) {
        return new LearnerId(value);
    }
}
