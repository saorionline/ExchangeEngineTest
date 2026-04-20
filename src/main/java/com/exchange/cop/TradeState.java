package com.exchange.cop;

/**
 * Lifecycle state of a single trade order.
 *
 * <pre>
 *   PENDING ──► CONFIRMED ──► RELEASED
 * </pre>
 *
 * <ul>
 *   <li>{@link #PENDING}   – Order listed; COP transfer not yet confirmed.</li>
 *   <li>{@link #CONFIRMED} – COP receipt verified; USDT may now be released.</li>
 *   <li>{@link #RELEASED}  – USDT sent to buyer; trade closed.</li>
 * </ul>
 */
public enum TradeState {
    PENDING,
    CONFIRMED,
    RELEASED
}
