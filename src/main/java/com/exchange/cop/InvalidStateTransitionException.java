package com.exchange.cop;

/**
 * Thrown when the state machine detects an illegal transition attempt.
 *
 * <p>Example: trying to {@code releaseUsdt} before the COP transfer has been
 * confirmed ({@link TradeState#PENDING} → {@link TradeState#RELEASED} is
 * forbidden; the order must pass through {@link TradeState#CONFIRMED} first).
 */
public class InvalidStateTransitionException extends RuntimeException {

    private final TradeState from;
    private final TradeState to;

    public InvalidStateTransitionException(TradeState from, TradeState to) {
        super(String.format(
            "Illegal state transition: %s → %s. " +
            "COP receipt must be confirmed before USDT is released.", from, to));
        this.from = from;
        this.to   = to;
    }

    public TradeState getFrom() { return from; }
    public TradeState getTo()   { return to;   }
}
