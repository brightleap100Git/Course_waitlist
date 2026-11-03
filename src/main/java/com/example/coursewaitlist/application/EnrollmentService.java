package com.example.coursewaitlist.application;

import com.example.coursewaitlist.domain.ActiveRegistration;
import com.example.coursewaitlist.domain.EnrollmentId;
import com.example.coursewaitlist.domain.EligibilityResult;
import com.example.coursewaitlist.domain.LearnerId;
import com.example.coursewaitlist.domain.RegistrationKind;
import com.example.coursewaitlist.domain.RegistrationOutcome;
import com.example.coursewaitlist.domain.RegistrationResult;
import com.example.coursewaitlist.persistence.RegistrationRepository;
import java.util.Objects;

public final class EnrollmentService {
    private final RegistrationRepository registrationRepository;
    private final PrerequisiteValidator prerequisiteValidator;
    private final PromotionCoordinator promotionCoordinator;
    private final WaitlistManager waitlistManager;

    public EnrollmentService(
            RegistrationRepository registrationRepository,
            PrerequisiteValidator prerequisiteValidator,
            PromotionCoordinator promotionCoordinator,
            WaitlistManager waitlistManager) {
        this.registrationRepository =
                Objects.requireNonNull(registrationRepository, "registrationRepository");
        this.prerequisiteValidator =
                Objects.requireNonNull(prerequisiteValidator, "prerequisiteValidator");
        this.promotionCoordinator =
                Objects.requireNonNull(promotionCoordinator, "promotionCoordinator");
        this.waitlistManager = Objects.requireNonNull(waitlistManager, "waitlistManager");
    }

    public RegistrationResult enroll(LearnerId learnerId) {
        if (learnerId == null) {
            return new RegistrationResult(RegistrationOutcome.INVALID_REQUEST);
        }

        return registrationRepository.transact(
                transaction -> {
                    var activeRegistration = transaction.findActive(learnerId);
                    if (activeRegistration.isPresent()) {
                        return duplicateOutcome(activeRegistration.orElseThrow());
                    }

                    EligibilityResult eligibility = prerequisiteValidator.check(learnerId);
                    if (!eligibility.eligible()) {
                        return new RegistrationResult(RegistrationOutcome.INELIGIBLE);
                    }

                    promotionCoordinator.allocateAvailableSeats(transaction);
                    if (transaction.countEnrolled() < transaction.getOffering().capacity()) {
                        transaction.saveEnrollment(learnerId);
                        return new RegistrationResult(RegistrationOutcome.ENROLLED);
                    }

                    waitlistManager.enqueue(transaction, learnerId);
                    return new RegistrationResult(RegistrationOutcome.WAITLISTED);
                });
    }

    public RegistrationResult drop(LearnerId learnerId) {
        if (learnerId == null) {
            return new RegistrationResult(RegistrationOutcome.INVALID_REQUEST);
        }

        return registrationRepository.transact(
                transaction -> {
                    var activeRegistration = transaction.findActive(learnerId);
                    if (activeRegistration.isEmpty()
                            || activeRegistration.orElseThrow().kind()
                                    != RegistrationKind.ENROLLED) {
                        return new RegistrationResult(RegistrationOutcome.NOT_ENROLLED);
                    }

                    ActiveRegistration enrollment = activeRegistration.orElseThrow();
                    boolean removed =
                            transaction.removeEnrollment(
                                    new EnrollmentId(enrollment.recordId()));
                    if (!removed) {
                        throw new IllegalStateException("Enrollment no longer exists");
                    }

                    promotionCoordinator.allocateAvailableSeats(transaction);
                    return new RegistrationResult(RegistrationOutcome.DROPPED);
                });
    }

    private static RegistrationResult duplicateOutcome(ActiveRegistration activeRegistration) {
        RegistrationOutcome outcome =
                activeRegistration.kind() == RegistrationKind.ENROLLED
                        ? RegistrationOutcome.ALREADY_ENROLLED
                        : RegistrationOutcome.ALREADY_WAITLISTED;
        return new RegistrationResult(outcome);
    }
}
