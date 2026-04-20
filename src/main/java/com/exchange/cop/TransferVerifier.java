package com.exchange.cop;

/**
 * Verifies that the COP transfer has landed in the seller's account.
 *
 * <p>Step 04–05: before releasing USDT the engine calls
 * {@link #verify(TradeResult)} to confirm receipt. In production this
 * would poll the bank or Nequi API; here it is a deterministic mock.
 */
public final class TransferVerifier {

    /**
     * Returns {@code true} when the COP transfer is confirmed for
     * the given result.
     *
     * <p>Implementation contract: a result in {@link TradeState#CONFIRMED}
     * or {@link TradeState#RELEASED} is considered verified; a result in
     * {@link TradeState#PENDING} is not.
     */
    public boolean verify(TradeResult result) {
        return result.getState() == TradeState.CONFIRMED
            || result.getState() == TradeState.RELEASED;
    }
}
