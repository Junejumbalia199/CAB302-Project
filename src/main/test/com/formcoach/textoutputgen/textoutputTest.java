package com.formcoach.textoutputgen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class textoutputTest {

    private textoutputgen generator;

    @BeforeEach
    void setup() {
        generator = new textoutputgen();
        generator.disableFlavourText = true;
    }

    // =========================================================
    // BASE POSES
    // =========================================================

    private Double[] basePoseX() {
        Double[] x = new Double[33];
        Arrays.fill(x, 0.0);

        x[11] = -0.5;
        x[12] = 0.5;
        x[13] = -0.7;
        x[14] = 0.7;
        x[15] = -0.9;
        x[16] = 0.9;
        x[23] = -0.4;
        x[24] = 0.4;
        x[25] = -0.4;
        x[26] = 0.4;
        x[27] = -0.4;
        x[28] = 0.4;

        return x;
    }

    private Double[] basePoseY() {
        Double[] y = new Double[33];
        Arrays.fill(y, 0.0);

        y[11] = 1.0;
        y[12] = 1.0;
        y[13] = 0.6;
        y[14] = 0.6;
        y[15] = 0.2;
        y[16] = 0.2;
        y[23] = -0.2;
        y[24] = -0.2;
        y[25] = -0.8;
        y[26] = -0.8;
        y[27] = -1.4;
        y[28] = -1.4;

        return y;
    }

    private Double[] basePoseZ() {
        Double[] z = new Double[33];
        Arrays.fill(z, 0.0);
        return z;
    }

    private Double[] copy(Double[] arr) {
        return Arrays.copyOf(arr, arr.length);
    }

    private textoutputgen.PoseResult run(Double[] x, Double[] y, Double[] z, String type) {

        return generator.output(x, y, z, basePoseX(), basePoseY(), basePoseZ(), type);
    }

    // =========================================================
    // EXCEPTION TESTS
    // =========================================================

    @Test
    void testNullInput() {

        PoseValidationException ex = assertThrows(PoseValidationException.class, () -> generator.output(null, null, null, null, null, null, "Pushup"));

        assertEquals("NULL_INPUT", ex.getErrorCode());
    }

    @Test
    void testNaNInput() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        x[15] = Double.NaN;

        PoseValidationException ex = assertThrows(PoseValidationException.class, () -> generator.output(x, y, z, basePoseX(), basePoseY(), basePoseZ(), "Pushup"));

        assertEquals("INVALID_VALUE", ex.getErrorCode());
    }

    @Test
    void testInvalidExercise() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        PoseValidationException ex = assertThrows(PoseValidationException.class, () -> generator.output(x, y, z, x, y, z, "Running"));

        assertEquals("INVALID_EXERCISE", ex.getErrorCode());
    }

    // =========================================================
    // CORE VALIDATION
    // =========================================================

    @Test
    void testPerfectInput() {

        var result = run(basePoseX(), basePoseY(), basePoseZ(), "Pushup");

        assertEquals(generator.PerfectText, result.summaryText());
        assertEquals(3, result.severity());
        assertEquals(10.0, result.score());
    }

    @Test
    void testVeryBadInput() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        y[13] += 0.60;

        var result = run(x, y, z, "Pushup");

        assertEquals(generator.VBadText, result.summaryText());
        assertEquals(0, result.severity());
    }

    // =========================================================
    // SCORE TESTS
    // =========================================================

    @Test
    void testScoreRange() {

        var result = run(basePoseX(), basePoseY(), basePoseZ(), "Pushup");

        assertTrue(result.score() >= 0);
        assertTrue(result.score() <= 10);
    }

    // =========================================================
    // MOVEMENT TESTS
    // =========================================================

    @Test
    void testDirectionUp() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        y[13] += 0.2;

        var result = run(x, y, z, "Pushup");

        assertTrue(result.movementFeedback().stream().anyMatch(s -> s.contains("up")));
    }

    @Test
    void testCombinedDirections() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        x[13] -= 0.2;
        y[13] += 0.2;

        var result = run(x, y, z, "Pushup");

        String combined = String.join(" ", result.movementFeedback());

        assertTrue(combined.contains("up"));
        assertTrue(combined.contains("right"));
    }

    // =========================================================
    // FLAVOUR TESTS
    // =========================================================

    @Test
    void testFlavourDisabled() {

        generator.disableFlavourText = true;

        var result = run(basePoseX(), basePoseY(), basePoseZ(), "Pushup");

        assertNull(result.flavourText());
    }

    @Test
    void testFlavourTextFromList() {

        generator.disableFlavourText = false;

        var result = run(basePoseX(), basePoseY(), basePoseZ(), "Pushup");

        assertTrue(Arrays.asList(generator.FlavourText).contains(result.flavourText()));
    }

    // =========================================================
    // PRINT TESTS
    // =========================================================

    @Test
    void testPrintPoseResult() {

        var result = run(basePoseX(), basePoseY(), basePoseZ(), "Pushup");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PrintStream originalOut = System.out;

        // Create a stream that writes to both console and capture stream
        PrintStream teeStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                originalOut.write(b);      // print to console
                outputStream.write(b);     // capture for assertions
            }
        });

        System.setOut(teeStream);

        try {
            generator.printPoseResult(result);
        } finally {
            System.setOut(originalOut);
        }

        String printed = outputStream.toString();

        assertTrue(printed.contains(result.summaryText()));
        assertTrue(printed.contains("Score"));
        assertTrue(printed.contains(String.valueOf(result.score())));
        assertTrue(printed.contains("Severity"));
        assertTrue(printed.contains(String.valueOf(result.severity())));
    }

    @Test
    void testInfiniteInput() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        x[15] = Double.POSITIVE_INFINITY;

        PoseValidationException ex = assertThrows(PoseValidationException.class, () -> generator.output(x, y, z, basePoseX(), basePoseY(), basePoseZ(), "Pushup"));

        assertEquals("INVALID_VALUE", ex.getErrorCode());
    }

    @Test
    void testPerfectPoseHasNoFeedback() {

        var result = run(basePoseX(), basePoseY(), basePoseZ(), "Pushup");

        assertTrue(result.movementFeedback().isEmpty());
    }

    @Test
    void testToleranceBoundaryGood() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        y[13] += 0.05;

        var result = run(x, y, z, "Pushup");

        assertEquals(3, result.severity());
    }

    @Test
    void testToleranceBoundarySlightAdjustment() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        y[13] += 0.051;

        var result = run(x, y, z, "Pushup");

        assertEquals(2, result.severity());
    }

    @Test
    void testMultipleBodyPartsFeedback() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        y[13] += 0.2;
        y[25] -= 0.2;

        var result = run(x, y, z, "Pushup");

        String feedback = String.join(" ", result.movementFeedback());

        assertTrue(feedback.contains("elbow"));
        assertTrue(feedback.contains("knee"));
    }

    @Test
    void testSkippedLandmarksIgnored() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        y[0] += 10.0;

        var result = run(x, y, z, "Pushup");

        assertEquals(3, result.severity());
        assertTrue(result.movementFeedback().isEmpty());
    }

    @Test
    void testForwardBackwardFeedback() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        z[13] += 0.2;

        var result = run(x, y, z, "Pushup");

        String feedback = String.join(" ", result.movementFeedback());

        assertTrue(feedback.contains("forward"));
    }

    @Test
    void testZeroShoulderWidthDoesNotCrash() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        x[11] = 0.0;
        x[12] = 0.0;

        assertDoesNotThrow(() -> run(x, y, z, "Pushup"));
    }

    @Test
    void testSitupAccepted() {

        var result = run(basePoseX(), basePoseY(), basePoseZ(), "Situp");

        assertNotNull(result);
    }

    // =========================================================
    // SCORE TESTS
    // =========================================================

    @Test
    void testSingleMinorErrorReducesScoreSlightly() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        // small tolerance drop
        y[13] += 0.051;

        var result = run(x, y, z, "Pushup");

        assertTrue(result.score() < 10.0);
        assertTrue(result.score() > 9.0);
    }

    @Test
    void testLargeErrorReducesScoreMore() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        y[13] += 0.20;

        var result = run(x, y, z, "Pushup");

        assertTrue(result.score() < 9.8);
    }

    @Test
    void testMultipleErrorsReduceScoreFurther() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        y[13] += 0.20;
        y[25] -= 0.20;
        z[15] += 0.20;

        var result = run(x, y, z, "Pushup");

        assertTrue(result.score() < 9.5);
    }

    @Test
    void testVeryBadPoseProducesLowScore() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        x[13] += 1.0;
        y[13] += 1.0;
        z[13] += 1.0;

        x[14] += 1.0;
        y[14] += 1.0;
        z[14] += 1.0;

        var result = run(x, y, z, "Pushup");

        assertTrue(result.score() < 8.0);
    }

    @Test
    void testSkippedLandmarksDoNotAffectScore() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        // nose is skipped
        y[0] += 999.0;

        var result = run(x, y, z, "Pushup");

        assertEquals(10.0, result.score());
    }

    @Test
    void testExactAverageScoreCalculation() {

        Double[] x = basePoseX();
        Double[] y = basePoseY();
        Double[] z = basePoseZ();

        // One landmark becomes tolerance level 2
        y[13] += 0.051;

        var result = run(x, y, z, "Pushup");

        int totalCoordinates = 66; // 22 landmarks * 3 axes
        double expectedAverage = ((65 * 3.0) + 2.0) / totalCoordinates;

        double expectedScore = Math.round((expectedAverage / 3.0) * 10.0 * 10.0) / 10.0;

        assertEquals(expectedScore, result.score());
    }
}