package com.exchange.cop;

/**
 * High-level phase of the exchange relationship.
 * <ul>
 *   <li>{@link #PILOT}    – Step 05: small test trade ($1K–$3K).</li>
 *   <li>{@link #SCALING}  – Step 06: large-volume deal ($20K+).</li>
 *   <li>{@link #FINALIZED}– Trade fully settled and archived.</li>
 * </ul>
 */
public enum ExchangePhase {
    PILOT,
    SCALING,
    FINALIZED
}
