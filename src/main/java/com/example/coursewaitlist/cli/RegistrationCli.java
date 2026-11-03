package com.example.coursewaitlist.cli;

import com.example.coursewaitlist.application.EnrollmentService;
import com.example.coursewaitlist.domain.Enrollment;
import com.example.coursewaitlist.domain.LearnerId;
import com.example.coursewaitlist.domain.RegistrationOutcome;
import com.example.coursewaitlist.domain.RegistrationResult;
import com.example.coursewaitlist.domain.WaitlistEntry;
import com.example.coursewaitlist.persistence.RegistrationRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Locale;
import java.util.Objects;

public final class RegistrationCli {
    private final EnrollmentService enrollmentService;
    private final RegistrationRepository registrationRepository;
    private final BufferedReader input;
    private final PrintWriter output;

    public RegistrationCli(
            EnrollmentService enrollmentService,
            RegistrationRepository registrationRepository,
            Reader input,
            Writer output) {
        this.enrollmentService = Objects.requireNonNull(enrollmentService, "enrollmentService");
        this.registrationRepository =
                Objects.requireNonNull(registrationRepository, "registrationRepository");
        this.input = new BufferedReader(Objects.requireNonNull(input, "input"));
        this.output = new PrintWriter(Objects.requireNonNull(output, "output"), true);
    }

    public void run() throws IOException {
        output.println("Course Registration CLI. Type HELP for commands.");
        String line;
        while ((line = input.readLine()) != null) {
            if (line.isBlank()) {
                output.println("Please enter a command. Type HELP for commands.");
                continue;
            }

            String[] parts = line.trim().split("\\s+");
            switch (parts[0].toUpperCase(Locale.ROOT)) {
                case "ENROLL" -> handleRegistration(parts, true);
                case "DROP" -> handleRegistration(parts, false);
                case "VIEW" -> handleView(parts);
                case "HELP" -> handleHelp(parts);
                case "EXIT" -> {
                    if (parts.length == 1) {
                        output.println("Goodbye.");
                        return;
                    }
                    output.println("INVALID_REQUEST: EXIT takes no arguments.");
                }
                default -> output.println("Unknown command. Type HELP for commands.");
            }
        }
        output.println("End of input. Exiting.");
    }

    private void handleRegistration(String[] parts, boolean enrollment) {
        if (parts.length > 2) {
            output.println(
                    "INVALID_REQUEST: Use "
                            + (enrollment ? "ENROLL <learner-id>." : "DROP <learner-id>."));
            return;
        }

        LearnerId learnerId = parts.length == 2 ? new LearnerId(parts[1]) : null;
        RegistrationResult result =
                enrollment
                        ? enrollmentService.enroll(learnerId)
                        : enrollmentService.drop(learnerId);
        printResult(result.outcome());
    }

    private void printResult(RegistrationOutcome outcome) {
        switch (outcome) {
            case ENROLLED -> output.println("ENROLLED: Enrollment confirmed.");
            case WAITLISTED -> output.println("WAITLISTED: Added to the FIFO waitlist.");
            case INELIGIBLE -> output.println("INELIGIBLE: Prerequisite requirements not met.");
            case ALREADY_ENROLLED ->
                    output.println("DUPLICATE: Learner is already enrolled.");
            case ALREADY_WAITLISTED ->
                    output.println("DUPLICATE: Learner is already on the waitlist.");
            case DROPPED -> output.println("DROPPED: Enrollment removed.");
            case NOT_ENROLLED -> output.println("NOT_ENROLLED: No active enrollment to drop.");
            case INVALID_REQUEST ->
                    output.println("INVALID_REQUEST: A valid learner ID is required.");
        }
    }

    private void handleView(String[] parts) {
        if (parts.length != 1) {
            output.println("Malformed command. Use VIEW with no arguments.");
            return;
        }

        var snapshot = registrationRepository.snapshot();
        output.println("Offering capacity: " + snapshot.offering().capacity());
        output.println("Enrolled learners (" + snapshot.enrolledCount() + "):");
        if (snapshot.enrollments().isEmpty()) {
            output.println("  (none)");
        } else {
            for (Enrollment enrollment : snapshot.enrollments()) {
                output.println("  " + enrollment.learnerId().value());
            }
        }
        output.println("Waitlisted learners (FIFO):");
        if (snapshot.waitlist().isEmpty()) {
            output.println("  (none)");
        } else {
            for (WaitlistEntry entry : snapshot.waitlist()) {
                output.println("  " + entry.position() + ": " + entry.learnerId().value());
            }
        }
        output.println(
                "Available seats: "
                        + (snapshot.offering().capacity() - snapshot.enrolledCount()));
    }

    private void handleHelp(String[] parts) {
        if (parts.length != 1) {
            output.println("Malformed command. Use HELP with no arguments.");
            return;
        }
        output.println("Commands:");
        output.println("  ENROLL <learner-id>  Request enrollment");
        output.println("  DROP <learner-id>    Drop an enrollment");
        output.println("  VIEW                 Display enrollment and FIFO waitlist state");
        output.println("  HELP                 Show available commands");
        output.println("  EXIT                 Leave the application");
    }
}
