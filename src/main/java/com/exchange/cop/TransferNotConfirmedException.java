package com.exchange.cop;

/**
 * Thrown when an attempt is made to release USDT before the COP bank/Nequi
 * transfer has been verified (Step 04–05 safety guard).
 */
public class TransferNotConfirmedException extends RuntimeException {

    public TransferNotConfirmedException(String tradeId) {
        super(String.format(
            "COP transfer for trade '%s' has not been confirmed. " +
            "Call confirmTransfer() before releaseUsdt().", tradeId));
    }
}
