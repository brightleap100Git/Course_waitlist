package com.example.coursewaitlist.cli;

import com.example.coursewaitlist.application.DemoPrerequisiteValidator;
import com.example.coursewaitlist.application.EnrollmentService;
import com.example.coursewaitlist.application.PromotionCoordinator;
import com.example.coursewaitlist.application.WaitlistManager;
import com.example.coursewaitlist.domain.EligibilityData;
import com.example.coursewaitlist.domain.LearnerId;
import com.example.coursewaitlist.domain.Offering;
import com.example.coursewaitlist.persistence.InMemoryRegistrationRepository;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

public final class DemoConfiguration {
    private static final int OFFERING_CAPACITY = 2;
    private static final Set<String> ELIGIBLE_LEARNER_IDS =
            Set.of("eligible-1", "eligible-2", "eligible-3", "eligible-4");

    private DemoConfiguration() {}

    public static RegistrationCli createCli(InputStream input, OutputStream output) {
        return createCli(
                new InputStreamReader(input, StandardCharsets.UTF_8),
                new OutputStreamWriter(output, StandardCharsets.UTF_8));
    }

    public static RegistrationCli createCli(java.io.Reader input, java.io.Writer output) {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository(new Offering(OFFERING_CAPACITY));
        DemoPrerequisiteValidator validator =
                new DemoPrerequisiteValidator(
                        new EligibilityData(
                                ELIGIBLE_LEARNER_IDS.stream()
                                        .map(LearnerId::new)
                                        .collect(Collectors.toUnmodifiableSet())));
        WaitlistManager waitlistManager = new WaitlistManager();
        PromotionCoordinator promotionCoordinator =
                new PromotionCoordinator(validator, waitlistManager);
        EnrollmentService enrollmentService =
                new EnrollmentService(
                        repository, validator, promotionCoordinator, waitlistManager);
        return new RegistrationCli(enrollmentService, repository, input, output);
    }
}
