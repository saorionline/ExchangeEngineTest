package com.exchange.cop;

/**
 * Computes a composite trust score for a {@link Counterpart}.
 *
 * <p>Step 02 – scores are used to gate whether a buyer is eligible for
 * a sell order. The formula weights both the feedback rating and the
 * completion rate to produce a 0–100 score.
 *
 * <pre>
 *   score = (rating / 5.0) * 60  +  (completionRate / 100.0) * 40
 * </pre>
 */
public final class TrustScorer {

    /** Minimum composite trust score to proceed. */
    public static final double MIN_TRUST_SCORE = 85.0;

    /**
     * Computes the trust score for the given counterpart.
     *
     * @param counterpart buyer profile (non-null)
     * @return composite score in the range [0, 100]
     */
    public double score(Counterpart counterpart) {
        double ratingComponent     = (counterpart.getRating() / 5.0) * 60.0;
        double completionComponent = (counterpart.getCompletionRate() / 100.0) * 40.0;
        return ratingComponent + completionComponent;
    }

    /**
     * Returns {@code true} when the counterpart meets the minimum trust
     * threshold to participate in a sell order.
     */
    public boolean isTrusted(Counterpart counterpart) {
        return score(counterpart) >= MIN_TRUST_SCORE;
    }
}
