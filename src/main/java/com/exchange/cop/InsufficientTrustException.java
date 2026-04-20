package com.exchange.cop;

/**
 * Thrown when a buyer's trust profile does not meet the minimum thresholds
 * required to proceed with a P2P listing (Step 01–02 guard).
 *
 * <p>Minimum requirements enforced by {@link ExchangeEngine}:
 * <ul>
 *   <li>Rating ≥ {@value ExchangeEngine#MIN_BUYER_RATING}</li>
 *   <li>Completed trades ≥ {@value ExchangeEngine#MIN_BUYER_TRADES}</li>
 * </ul>
 */
public class InsufficientTrustException extends RuntimeException {

    private final String buyerUsername;

    public InsufficientTrustException(String buyerUsername, String reason) {
        super(String.format("Buyer '%s' does not meet trust requirements: %s",
                            buyerUsername, reason));
        this.buyerUsername = buyerUsername;
    }

    public String getBuyerUsername() { return buyerUsername; }
}
