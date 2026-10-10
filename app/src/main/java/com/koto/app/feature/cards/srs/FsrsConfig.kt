package com.koto.app.feature.cards.srs

/**
 * Single source of truth for all FSRS parameters and thresholds.
 * Magic numbers and thresholds must NEVER be scattered across files.
 */
object FsrsConfig {
    /** Target retention rate across reviews (default 0.9 = 90%). */
    const val REQUEST_RETENTION: Double = 0.9

    /** Maximum allowed interval in days. */
    const val MAXIMUM_INTERVAL_DAYS: Int = 36500

    /**
     * Mastered threshold:
     * Card must be in Review state and have stability >= 21.0 days.
     */
    const val MASTERED_STABILITY_DAYS: Double = 21.0

    /**
     * Weak threshold:
     * Card is not New, and (state == Relearning OR lapses >= 2 OR last_rating == Again).
     */
    const val WEAK_LAPSE_THRESHOLD: Int = 2

    /**
     * Intra-session short-step threshold (~10 minutes in milliseconds).
     * Again ratings with due interval within this threshold get appended
     * to the active session queue for immediate reinforcement.
     */
    const val SHORT_STEP_THRESHOLD_MS: Long = 10 * 60 * 1000L

    /** Day in milliseconds for UTC timestamp math. */
    const val DAY_MS: Long = 24L * 60 * 60 * 1000L

    /**
     * Official default weights (21 parameters) for FSRS v6.
     */
    val DEFAULT_W: DoubleArray = doubleArrayOf(
        0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001,
        1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014,
        1.8729, 0.5425, 0.0912, 0.0658, 0.1542
    )

    /** Learning steps in minutes: Step 1 = 1m, Step 2 = 10m. */
    val LEARNING_STEPS_MINUTES: List<Int> = listOf(1, 10)

    /** Relearning steps in minutes: Step 1 = 10m. */
    val RELEARNING_STEPS_MINUTES: List<Int> = listOf(10)
}
