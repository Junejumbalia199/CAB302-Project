package com.formcoach.textoutputgen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        return generator.output(x, y, z, x, y, z, type);
    }

    // =========================================================
    // EXCEPTION TESTS (UPDATED)
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
    // CORE VALIDATION STILL VALID
    // =========================================================

    @Test
    void testPerfectInput() {

        var result = run(basePoseX(), basePoseY(), basePoseZ(), "Pushup");

        assertEquals(generator.PerfectText, result.summaryText());
        assertEquals(3, result.severity());
        assertTrue(result.score() >= 9.0);
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
        assertTrue(combined.contains("left"));
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
    void testFlavourEnabled() {

        generator.disableFlavourText = false;

        var result = run(basePoseX(), basePoseY(), basePoseZ(), "Pushup");

        assertNotNull(result.flavourText());
    }
}