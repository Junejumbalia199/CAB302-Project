package com.formcoach.repcount;

/**
 * Counts exercise repetitions by tracking joint angle transitions through a
 * two-phase state machine.
 *
 * <p>Feed the 7-element angle array from {@code PoseScorer.getLastAngles()} into
 * {@link #update(double[])} on every landmark frame. The method returns {@code true}
 * the moment a complete rep is detected (end-phase to start-phase transition).
 *
 * <p>Angle index mapping (matches PoseScorer order):
 * <pre>
 *   0 = right elbow angle
 *   1 = left  elbow angle
 *   2 = knee  angle (mid-hip as vertex)
 *   3 = right hip-knee-ankle
 *   4 = left  hip-knee-ankle
 *   5 = right wrist-elbow-shoulder
 *   6 = left  wrist-elbow-shoulder
 * </pre>
 *
 * <p>Rep thresholds per exercise:
 * <pre>
 *   Push-ups : watch a[0] (right elbow).  Start &gt; 150 deg, End &lt; 100 deg
 *   Squats   : watch a[2] (knee).         Start &gt; 150 deg, End &lt; 110 deg
 *   Sit-ups  : watch a[3] (right hip).    Start &gt; 130 deg, End &lt;  90 deg
 * </pre>
 */
public class RepetitionCounter {

    /**
     * Motion phases tracked by the state machine.
     */
    public enum Phase {
        /** Waiting for the user to reach the start position before tracking begins. */
        IDLE,
        /** User is at the top/start of the movement (e.g. arms extended for push-ups). */
        AT_START,
        /** User is at the bottom/end of the movement (e.g. arms bent for push-ups). */
        AT_END
    }

    /**
     * Number of consecutive frames an angle must stay past a threshold before
     * the phase transition is confirmed. Prevents jitter at the boundary.
     */
    private static final int CONFIRM_FRAMES = 3;

    // 10-degree hysteresis band applied to each threshold
    private static final double HYSTERESIS = 10.0;

    private Phase currentPhase = Phase.IDLE;
    private int   phaseFrameCount = 0;
    private int   repCount = 0;

    // per-exercise configuration
    private final int    angleIndex;
    private final double startThreshold;  // "top" of motion (e.g. arms extended)
    private final double endThreshold;    // "bottom" of motion (e.g. arms bent)

    /**
     * Creates a counter configured for the given exercise name.
     * Supported values (case-insensitive): "push-ups", "squats", "sit-ups".
     * Falls back to push-up thresholds for unrecognised names.
     *
     * @param exercise the exercise display name used in the app
     */
    public RepetitionCounter(String exercise) {
        if (exercise == null) exercise = "";
        switch (exercise.toLowerCase()) {
            case "squats":
                angleIndex      = 2;
                startThreshold  = 150.0;
                endThreshold    = 110.0;
                break;
            case "sit-ups":
                angleIndex      = 3;
                startThreshold  = 130.0;
                endThreshold    =  90.0;
                break;
            case "push-ups":
            default:
                angleIndex      = 0;
                startThreshold  = 150.0;
                endThreshold    = 100.0;
                break;
        }
    }

    /**
     * Updates the state machine with the latest joint angles.
     * Call this every landmark frame, immediately after {@code scorer.score()}.
     *
     * @param angles the 7-element array returned by {@code PoseScorer.getLastAngles()},
     *               or {@code null} to skip this frame
     * @return {@code true} if a complete repetition was just completed
     */
    public boolean update(double[] angles) {
        if (angles == null || angleIndex >= angles.length) return false;

        double angle  = angles[angleIndex];
        // apply hysteresis so transitions only trigger well inside each zone
        boolean atStart = angle > (startThreshold - HYSTERESIS);
        boolean atEnd   = angle < (endThreshold   + HYSTERESIS);

        switch (currentPhase) {
            case IDLE:
                // wait for the user to reach start position before tracking begins
                if (atStart) {
                    phaseFrameCount++;
                    if (phaseFrameCount >= CONFIRM_FRAMES) {
                        currentPhase    = Phase.AT_START;
                        phaseFrameCount = 0;
                    }
                } else {
                    phaseFrameCount = 0;
                }
                break;

            case AT_START:
                // user is at top — wait for them to reach the bottom
                if (atEnd) {
                    phaseFrameCount++;
                    if (phaseFrameCount >= CONFIRM_FRAMES) {
                        currentPhase    = Phase.AT_END;
                        phaseFrameCount = 0;
                    }
                } else {
                    phaseFrameCount = 0;
                }
                break;

            case AT_END:
                // user is at bottom — count the rep when they return to top
                if (atStart) {
                    phaseFrameCount++;
                    if (phaseFrameCount >= CONFIRM_FRAMES) {
                        currentPhase    = Phase.AT_START;
                        phaseFrameCount = 0;
                        repCount++;
                        return true;   // rep completed
                    }
                } else {
                    phaseFrameCount = 0;
                }
                break;
        }
        return false;
    }

    /**
     * Returns the total number of completed reps since creation or last {@link #reset()}.
     * @return the rep count
     */
    public int getRepCount() {
        return repCount;
    }

    /** Resets the rep count and state machine back to their initial state. */
    public void reset() {
        repCount        = 0;
        currentPhase    = Phase.IDLE;
        phaseFrameCount = 0;
    }
}
