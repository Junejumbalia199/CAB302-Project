package com.formcoach.textoutputgen;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class textoutputTest {

    private textoutputgen generator;

    @BeforeEach
    void setup() {
        generator = new textoutputgen();
        generator.disableFlavourText = true; // disable randomness
    }

    // helper to build arrays
    private Double[] arr(double val, int size) {
        Double[] a = new Double[size];
        for (int i = 0; i < size; i++) {
            a[i] = val;
        }
        return a;
    }

    @Test
    public void testForBadInput() {

        Double[] userX = arr(0.02, 33);
        Double[] idealX = arr(0.0, 33);

        Double[] userY = arr(0.02, 33);
        Double[] idealY = arr(0.0, 33);

        Double[] userZ = arr(0.02, 33);
        Double[] idealZ = arr(0.0, 33);

        String result = generator.output(
                userX, userY, userZ,
                idealX, idealY, idealZ,
                "Pushup"
        );

        System.out.println("\n=== Bad Input Test ===");
        System.out.println(result);

        assertTrue(result.contains(generator.BadText));
        assertFalse(result.contains(generator.PerfectText));
    }

    @Test
    public void testForGoodInput() {

        Double[] userX = arr(0.005, 33);
        Double[] idealX = arr(0.0, 33);

        Double[] userY = arr(0.005, 33);
        Double[] idealY = arr(0.0, 33);

        Double[] userZ = arr(0.005, 33);
        Double[] idealZ = arr(0.0, 33);

        String result = generator.output(
                userX, userY, userZ,
                idealX, idealY, idealZ,
                "Squat"
        );

        System.out.println("\n=== Good Input Test ===");
        System.out.println(result);

        assertTrue(result.contains(generator.GoodText));
        assertFalse(result.contains(generator.BadText));
    }

    @Test
    public void testForPerfectInput() {

        Double[] userX = arr(0.0, 33);
        Double[] idealX = arr(0.0, 33);

        Double[] userY = arr(0.0, 33);
        Double[] idealY = arr(0.0, 33);

        Double[] userZ = arr(0.0, 33);
        Double[] idealZ = arr(0.0, 33);

        String result = generator.output(
                userX, userY, userZ,
                idealX, idealY, idealZ,
                "Situp"
        );

        System.out.println("\n=== Perfect Input Test ===");
        System.out.println(result);

        assertTrue(result.contains(generator.PerfectText));
        assertFalse(result.contains(generator.BadText));
    }

    @Test
    public void testForVBadInput() {

        Double[] userX = arr(0.5, 33);
        Double[] idealX = arr(0.0, 33);

        Double[] userY = arr(0.5, 33);
        Double[] idealY = arr(0.0, 33);

        Double[] userZ = arr(0.5, 33);
        Double[] idealZ = arr(0.0, 33);

        String result = generator.output(
                userX, userY, userZ,
                idealX, idealY, idealZ,
                "Pushup"
        );

        System.out.println("\n=== Very Bad Input Test ===");
        System.out.println(result);

        assertTrue(result.contains(generator.VBadText));
        assertFalse(result.contains(generator.GoodText));
    }

    @Test
    public void testToleranceBoundaryBadVsGood() {
        Double[] user = arr(0.01, 33); // exactly at boundary
        Double[] ideal = arr(0.0, 33);

        String result = generator.output(user, user, user, ideal, ideal, ideal, "Pushup");

        System.out.println("\n=== Boundary Test (0.01) ===");
        System.out.println(result);

        // Decide what you EXPECT here and lock it in:
        assertTrue(result.contains(generator.BadText)
                || result.contains(generator.GoodText));
    }

    @Test
    public void testDirectionUp() {
        Double[] user = arr(0.02, 33);   // above ideal
        Double[] ideal = arr(0.0, 33);

        String result = generator.output(user, user, user, ideal, ideal, ideal, "Pushup");

        System.out.println("\n=== Direction Up Test ===");
        System.out.println(result);

        assertTrue(result.contains("up"));
    }

    @Test
    public void testDirectionDown() {
        Double[] user = arr(-0.02, 33);  // below ideal
        Double[] ideal = arr(0.0, 33);

        String result = generator.output(user, user, user, ideal, ideal, ideal, "Pushup");

        System.out.println("\n=== Direction Down Test ===");
        System.out.println(result);

        assertTrue(result.contains("down"));
    }

    @Test
    public void testSkipIndexWorks() {
        Double[] user = arr(0.02, 33);
        Double[] ideal = arr(0.0, 33);

        String result = generator.output(user, user, user, ideal, ideal, ideal, "Pushup");

        System.out.println("\n=== Skip Index Test ===");
        System.out.println(result);

        // Example: "nose" is skipped (index 0 = true)
        assertFalse(result.contains("nose"));
    }

    @Test
    public void testFlavourTextDisabled() {
        generator.disableFlavourText = true;

        Double[] user = arr(0.0, 33);
        Double[] ideal = arr(0.0, 33);

        String result = generator.output(user, user, user, ideal, ideal, ideal, "Pushup");

        for (String flavour : generator.FlavourText) {
            assertFalse(result.contains(flavour));
        }
    }

    @Test
    public void testFlavourTextEnabled() {
        generator.disableFlavourText = false;

        Double[] user = arr(0.0, 33);
        Double[] ideal = arr(0.0, 33);

        String result = generator.output(user, user, user, ideal, ideal, ideal, "Pushup");

        boolean found = false;
        for (String flavour : generator.FlavourText) {
            if (result.contains(flavour)) {
                found = true;
                break;
            }
        }

        assertTrue(found);
    }

    @Test
    public void testMixedTolerances() {
        Double[] user = arr(0.0, 33);
        Double[] ideal = arr(0.0, 33);

        user[12] = 0.5;   // very bad
        user[13] = 0.02;  // bad
        user[14] = 0.005; // good

        String result = generator.output(user, user, user, ideal, ideal, ideal, "Pushup");

        System.out.println("\n=== Mixed Tolerance Test ===");
        System.out.println(result);

        // Worst should dominate
        assertTrue(result.contains(generator.VBadText));
    }

    @Test
    public void testInvalidExerciseType() {
        Double[] user = arr(0.0, 33);
        Double[] ideal = arr(0.0, 33);

        assertThrows(Exception.class, () -> {
            generator.output(user, user, user, ideal, ideal, ideal, "Running");
        });
    }

    @Test
    public void testNullInput() {
        assertThrows(NullPointerException.class, () -> {
            generator.output(null, null, null, null, null, null, "Pushup");
        });
    }
}