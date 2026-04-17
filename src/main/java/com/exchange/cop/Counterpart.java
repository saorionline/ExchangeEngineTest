package com.exchange.cop;

import java.util.Objects;

/**
 * Represents a Binance P2P buyer (counterpart) profile.
 *
 * <p>Step 01 – before listing a sell order, the engine filters buyers
 * by their {@link #getRating()} and {@link #getCompletedTrades()}.
 *
 * <p>Use the factory method {@link #of(String, double, int)} to construct
 * validated instances.
 */
public final class Counterpart {

    private final String username;
    private final double rating;           // 0.0 – 5.0
    private final int    completedTrades;
    private final double completionRate;   // 0.0 – 100.0 (default 95 %)

    // ── private constructor ────────────────────────────────────────────────

    private Counterpart(String username, double rating,
                        int completedTrades, double completionRate) {
        this.username        = username;
        this.rating          = rating;
        this.completedTrades = completedTrades;
        this.completionRate  = completionRate;
    }

    // ── factory methods ────────────────────────────────────────────────────

    /**
     * Creates a {@code Counterpart} with a default completion rate of 95 %.
     *
     * @param username       Binance P2P username (non-null, non-blank)
     * @param rating         Feedback score, 0.0–5.0 inclusive
     * @param completedTrades Number of successfully completed trades (≥ 0)
     * @throws NullPointerException     if {@code username} is null
     * @throws IllegalArgumentException if any value is out of range
     */
    public static Counterpart of(String username, double rating, int completedTrades) {
        return of(username, rating, completedTrades, 95.0);
    }

    /**
     * Creates a {@code Counterpart} with an explicit completion rate.
     *
     * @param username        Binance P2P username (non-null, non-blank)
     * @param rating          Feedback score, 0.0–5.0 inclusive
     * @param completedTrades Number of successfully completed trades (≥ 0)
     * @param completionRate  Percentage of trades completed, 0.0–100.0
     */
    public static Counterpart of(String username, double rating,
                                 int completedTrades, double completionRate) {
        Objects.requireNonNull(username, "Username cannot be null");
        if (username.isBlank())
            throw new IllegalArgumentException("Username cannot be blank");
        if (rating < 0.0 || rating > 5.0)
            throw new IllegalArgumentException(
                "Rating must be between 0.0 and 5.0, was: " + rating);
        if (completedTrades < 0)
            throw new IllegalArgumentException(
                "Completed trades cannot be negative, was: " + completedTrades);
        if (completionRate < 0.0 || completionRate > 100.0)
            throw new IllegalArgumentException(
                "Completion rate must be between 0.0 and 100.0, was: " + completionRate);

        return new Counterpart(username, rating, completedTrades, completionRate);
    }

    // ── accessors ──────────────────────────────────────────────────────────

    public String getUsername()       { return username;        }
    public double getRating()         { return rating;          }
    public int    getCompletedTrades(){ return completedTrades; }
    public double getCompletionRate() { return completionRate;  }

    // ── Object ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("Counterpart{username='%s', rating=%.2f, trades=%d, completion=%.1f%%}",
            username, rating, completedTrades, completionRate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Counterpart)) return false;
        Counterpart c = (Counterpart) o;
        return username.equals(c.username);
    }

    @Override
    public int hashCode() { return Objects.hash(username); }
}