package com.exchange.cop;

/**
 * Payment method the seller uses to receive COP from the buyer.
 * Step 03 – agreed during the in-person meeting.
 */
public enum PaymentMethod {

    /** Traditional bank transfer (Bancolombia, Davivienda, etc.). */
    BANK_TRANSFER,

    /** Nequi mobile wallet — preferred for instant confirmation. */
    NEQUI
}
