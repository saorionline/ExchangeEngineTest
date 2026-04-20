package com.exchange.cop;

/**
 * Enforces legal state transitions for a {@link TradeResult}.
 *
 * <pre>
 *   PENDING ──► CONFIRMED ──► RELEASED
 * </pre>
 *
 * Any attempt to skip {@code CONFIRMED} and go directly from
 * {@code PENDING} to {@code RELEASED} throws
 * {@link InvalidStateTransitionException}.
 */
public final class StateMachine {

    private StateMachine() {}

    /**
     * Transitions {@code current} to {@link TradeState#CONFIRMED}.
     *
     * @throws InvalidStateTransitionException if current state is not PENDING
     */
    public static TradeResult confirm(TradeResult current, double copReceived) {
        if (current.getState() != TradeState.PENDING)
            throw new InvalidStateTransitionException(current.getState(), TradeState.CONFIRMED);
        return TradeResult.confirmed(current.getOrder(), copReceived);
    }

    /**
     * Transitions {@code current} to {@link TradeState#RELEASED}.
     *
     * @throws InvalidStateTransitionException if current state is not CONFIRMED
     */
    public static TradeResult release(TradeResult current) {
        if (current.getState() != TradeState.CONFIRMED)
            throw new InvalidStateTransitionException(current.getState(), TradeState.RELEASED);
        return TradeResult.released(current);
    }
}
