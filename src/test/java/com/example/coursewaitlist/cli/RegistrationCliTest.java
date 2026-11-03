package com.example.coursewaitlist.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.coursewaitlist.Main;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RegistrationCliTest {
    @Test
    void mainEntryPointInitializesAndExitsTheRunnableApplication() throws IOException {
        var originalInput = System.in;
        var originalOutput = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        try (var testOutput =
                new java.io.PrintStream(capturedOutput, true, StandardCharsets.UTF_8)) {
            System.setIn(
                    new ByteArrayInputStream(
                            "VIEW\nEXIT\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(testOutput);

            Main.main(new String[0]);
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
        }

        String output = capturedOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Course Registration CLI."));
        assertTrue(output.contains("Offering capacity: 2"));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void initializesTheDocumentedDemoAndStartsCleanly() throws IOException {
        String output = run("VIEW\nEXIT\n");

        assertTrue(output.contains("Course Registration CLI."));
        assertTrue(output.contains("Offering capacity: 2"));
        assertTrue(output.contains("Enrolled learners (0):"));
        assertTrue(output.contains("Waitlisted learners (FIFO):"));
        assertTrue(output.contains("Available seats: 2"));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void testTP01EnrollsAnEligibleLearnerThroughTheService() throws IOException {
        String output = run("ENROLL eligible-1\nVIEW\nEXIT\n");

        assertTrue(output.contains("ENROLLED: Enrollment confirmed."));
        assertTrue(output.contains("Enrolled learners (1):"));
        assertTrue(output.contains("  eligible-1"));
        assertTrue(output.contains("Available seats: 1"));
    }

    @Test
    void testTP02WaitlistsAnEligibleLearnerWhenTheDemoOfferingIsFull() throws IOException {
        String output = run("ENROLL eligible-1\nENROLL eligible-2\nENROLL eligible-3\nVIEW\nEXIT\n");

        assertTrue(output.contains("WAITLISTED: Added to the FIFO waitlist."));
        assertTrue(output.contains("Waitlisted learners (FIFO):"));
        assertTrue(output.contains("  0: eligible-3"));
        assertTrue(output.contains("Available seats: 0"));
    }

    @Test
    void testTP03TP04TP05ReportsIneligibleAndDuplicateRequests() throws IOException {
        String output =
                run(
                        "ENROLL unknown-learner\n"
                                + "ENROLL eligible-1\n"
                                + "ENROLL eligible-1\n"
                                + "ENROLL eligible-2\n"
                                + "ENROLL eligible-3\n"
                                + "ENROLL eligible-3\n"
                                + "EXIT\n");

        assertTrue(output.contains("INELIGIBLE:"));
        assertTrue(output.contains("DUPLICATE: Learner is already enrolled."));
        assertTrue(output.contains("DUPLICATE: Learner is already on the waitlist."));
    }

    @Test
    void testTP29DropsAndPromotesWithoutInventingPromotionDetails()
            throws IOException {
        String output =
                run(
                        "ENROLL eligible-1\n"
                                + "ENROLL eligible-2\n"
                                + "ENROLL eligible-3\n"
                                + "DROP eligible-1\n"
                                + "VIEW\n"
                                + "EXIT\n");

        assertTrue(output.contains("DROPPED: Enrollment removed."));
        assertTrue(output.contains("Enrolled learners (2):"));
        assertTrue(output.contains("  eligible-3"));
        assertTrue(output.contains("Waitlisted learners (FIFO):"));
        assertTrue(output.contains("  (none)"));
        assertFalse(output.contains("PROMOTED:"));
    }

    @Test
    void testTP10TP12ReportsMissingLearnerAndNotEnrolled() throws IOException {
        String output =
                run(
                        "DROP absent-learner\n"
                                + "ENROLL\n"
                                + "DROP\n"
                                + "ENROLL eligible-1 extra\n"
                                + "EXIT\n");

        assertTrue(output.contains("NOT_ENROLLED:"));
        assertTrue(output.contains("INVALID_REQUEST: A valid learner ID is required."));
        assertTrue(output.contains("INVALID_REQUEST: Use ENROLL <learner-id>."));
    }

    @Test
    void testTP13ReportsRepeatedDropAfterSuccessfulDrop() throws IOException {
        String output = run("ENROLL eligible-1\nDROP eligible-1\nDROP eligible-1\nEXIT\n");

        assertTrue(output.contains("DROPPED: Enrollment removed."));
        assertTrue(output.contains("NOT_ENROLLED: No active enrollment to drop."));
    }

    @Test
    void handlesUnknownBlankAndMalformedCommandsWithoutExiting() throws IOException {
        String output =
                run(
                        "\nUNKNOWN\nVIEW extra\nHELP extra\nEXIT extra\n"
                                + "ENROLL eligible-1 extra\nDROP eligible-1 extra\n"
                                + "ENROLL eligible-1\nEXIT\n");

        assertTrue(output.contains("Please enter a command."));
        assertTrue(output.contains("Unknown command."));
        assertTrue(output.contains("Malformed command. Use VIEW with no arguments."));
        assertTrue(output.contains("Malformed command. Use HELP with no arguments."));
        assertTrue(output.contains("INVALID_REQUEST: EXIT takes no arguments."));
        assertTrue(output.contains("INVALID_REQUEST: Use DROP <learner-id>."));
        assertTrue(output.contains("ENROLLED: Enrollment confirmed."));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void helpListsCommandsAndExitTerminatesTheSession() throws IOException {
        String output = run("HELP\nEXIT\nENROLL eligible-1\n");

        assertTrue(output.contains("ENROLL <learner-id>"));
        assertTrue(output.contains("DROP <learner-id>"));
        assertTrue(output.contains("VIEW"));
        assertTrue(output.contains("EXIT"));
        assertFalse(output.contains("ENROLLED:"));
    }

    @Test
    void unexpectedEndOfInputExitsCleanly() throws IOException {
        String output = run("ENROLL eligible-1\n");

        assertTrue(output.contains("ENROLLED: Enrollment confirmed."));
        assertTrue(output.contains("End of input. Exiting."));
    }

    @Test
    void testTP14DisplaysWaitlistInFifoOrderAndReportsAvailableSeats() throws IOException {
        String output =
                run(
                        "ENROLL eligible-1\n"
                                + "ENROLL eligible-2\n"
                                + "ENROLL eligible-3\n"
                                + "ENROLL eligible-4\n"
                                + "VIEW\n"
                                + "EXIT\n");

        int first = output.indexOf("  0: eligible-3");
        int second = output.indexOf("  1: eligible-4");
        assertTrue(first >= 0);
        assertTrue(second > first);
        assertTrue(output.contains("Enrolled learners (2):"));
        assertTrue(output.contains("Available seats: 0"));
    }

    @Test
    void testTP29SequentialCommandsObserveStateChangesWithinOneSession() throws IOException {
        String output =
                run(
                        "ENROLL eligible-1\n"
                                + "ENROLL eligible-2\n"
                                + "ENROLL eligible-3\n"
                                + "DROP eligible-2\n"
                                + "VIEW\n"
                                + "EXIT\n");

        assertTrue(output.contains("DROPPED: Enrollment removed."));
        assertTrue(output.contains("Enrolled learners (2):"));
        assertTrue(output.contains("  eligible-3"));
        assertTrue(output.contains("Available seats: 0"));
    }

    private static String run(String commands) throws IOException {
        StringWriter output = new StringWriter();
        RegistrationCli cli =
                DemoConfiguration.createCli(new StringReader(commands), output);
        cli.run();
        return output.toString();
    }
}
