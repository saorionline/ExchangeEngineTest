package com.exchange.cop;

/**
 * Provides the USDT → COP conversion rate.
 *
 * <p>In production this would query a live price feed.
 * For this implementation a fixed reference rate is used so that
 * unit tests are deterministic.
 */
public final class FxRateProvider {

    /** Reference rate: 1 USDT = 4,100 COP (illustrative). */
    public static final double DEFAULT_RATE_COP_PER_USDT = 4_100.0;

    private final double ratePerUsdt;

    public FxRateProvider() {
        this(DEFAULT_RATE_COP_PER_USDT);
    }

    public FxRateProvider(double ratePerUsdt) {
        if (ratePerUsdt <= 0)
            throw new IllegalArgumentException("FX rate must be > 0, was: " + ratePerUsdt);
        this.ratePerUsdt = ratePerUsdt;
    }

    /**
     * Converts a USDT amount to COP.
     *
     * @param usdt amount in USDT (must be &gt; 0)
     * @return equivalent amount in COP
     */
    public double convert(double usdt) {
        if (usdt <= 0)
            throw new IllegalArgumentException("USDT amount must be > 0, was: " + usdt);
        return usdt * ratePerUsdt;
    }

    public double getRate() { return ratePerUsdt; }
}
