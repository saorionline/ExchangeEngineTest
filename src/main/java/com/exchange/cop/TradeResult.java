package com.exchange.cop;

import java.util.Objects;

/**
 * Immutable snapshot of a trade's outcome at a given point in its lifecycle.
 *
 * <p>Created by {@link ExchangeEngine} when an order is listed or executed;
 * mutated (as a new instance) by the {@link StateMachine} on each transition.
 */
public final class TradeResult {

    private final TradeOrder order;
    private final TradeState state;
    private final double     copReceived;  // 0 until CONFIRMED

    private TradeResult(TradeOrder order, TradeState state, double copReceived) {
        this.order       = order;
        this.state       = state;
        this.copReceived = copReceived;
    }

    // ── factory ────────────────────────────────────────────────────────────

    /** Creates a PENDING result (COP not yet received). */
    public static TradeResult pending(TradeOrder order) {
        Objects.requireNonNull(order, "TradeOrder cannot be null");
        return new TradeResult(order, TradeState.PENDING, 0.0);
    }

    /** Creates a CONFIRMED result with the COP amount credited. */
    public static TradeResult confirmed(TradeOrder order, double copReceived) {
        Objects.requireNonNull(order, "TradeOrder cannot be null");
        if (copReceived <= 0)
            throw new IllegalArgumentException(
                "COP received must be > 0, was: " + copReceived);
        return new TradeResult(order, TradeState.CONFIRMED, copReceived);
    }

    /** Creates a RELEASED result (USDT sent, trade closed). */
    public static TradeResult released(TradeResult confirmed) {
        Objects.requireNonNull(confirmed, "Source TradeResult cannot be null");
        if (confirmed.getState() != TradeState.CONFIRMED)
            throw new InvalidStateTransitionException(confirmed.getState(), TradeState.RELEASED);
        return new TradeResult(confirmed.order, TradeState.RELEASED, confirmed.copReceived);
    }

    // ── accessors ──────────────────────────────────────────────────────────

    public TradeOrder getOrder()       { return order;       }
    public TradeState getState()       { return state;       }
    public double     getCopReceived() { return copReceived; }
    public String     getTradeId()     { return order.getId(); }

    // ── toString ───────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("TradeResult{id='%s', state=%s, cop=%.2f}",
            order.getId(), state, copReceived);
    }
}
