package com.exchange.cop;

import java.util.Objects;

/**
 * Central orchestrator of the USDT → COP P2P exchange workflow.
 *
 * <h2>Six-step lifecycle</h2>
 * <ol>
 *   <li>List a sell order on Binance P2P ({@link #listSellOrder}).</li>
 *   <li>Trust guard — buyer must meet rating and trade-count thresholds.</li>
 *   <li>Order details recorded (payment method, phase).</li>
 *   <li>COP transfer confirmed ({@link #confirmTransfer}).</li>
 *   <li>USDT released ({@link #releaseUsdt}).</li>
 *   <li>Result persisted to the {@link ExchangeLedger}.</li>
 * </ol>
 *
 * <p><strong>Safety invariant:</strong> {@link #releaseUsdt} will throw
 * {@link InvalidStateTransitionException} if called before
 * {@link #confirmTransfer}. This mirrors the real-world rule of never
 * sending crypto before verifying fiat receipt.
 */
public final class ExchangeEngine {

    // ── Trust thresholds (Step 01–02) ─────────────────────────────────────

    /** Minimum Binance P2P feedback rating accepted. */
    public static final double MIN_BUYER_RATING = 4.5;

    /** Minimum number of completed P2P trades accepted. */
    public static final int    MIN_BUYER_TRADES = 50;

    // ── Dependencies ──────────────────────────────────────────────────────

    private final ExchangeLedger  ledger;
    private final FxRateProvider  fxRate;
    private final AuditLogger     auditLog;

    // ── Construction ──────────────────────────────────────────────────────

    private ExchangeEngine(FxRateProvider fxRate) {
        this.ledger   = new ExchangeLedger();
        this.fxRate   = fxRate;
        this.auditLog = new AuditLogger();
    }

    /** Creates an engine with the default FX rate. */
    public static ExchangeEngine create() {
        return new ExchangeEngine(new FxRateProvider());
    }

    /** Creates an engine with a custom FX rate (useful for testing). */
    public static ExchangeEngine create(FxRateProvider fxRate) {
        Objects.requireNonNull(fxRate, "FxRateProvider cannot be null");
        return new ExchangeEngine(fxRate);
    }

    // ── Step 01: List sell order ───────────────────────────────────────────

    /**
     * Lists a USDT sell order on Binance P2P after validating the buyer.
     *
     * <p>Validation rules:
     * <ul>
     *   <li>Buyer rating ≥ {@value #MIN_BUYER_RATING}</li>
     *   <li>Buyer completed trades ≥ {@value #MIN_BUYER_TRADES}</li>
     * </ul>
     *
     * @param order fully-built {@link TradeOrder} (non-null)
     * @return a {@link TradeResult} in {@link TradeState#PENDING} state
     * @throws InsufficientTrustException if buyer does not meet thresholds
     * @throws NullPointerException       if order is null
     */
    public TradeResult listSellOrder(TradeOrder order) {
        Objects.requireNonNull(order, "TradeOrder cannot be null");
        validateBuyer(order.getCounterpart());

        TradeResult result = TradeResult.pending(order);
        ledger.add(result);
        auditLog.log(result, "Sell order listed on Binance P2P");
        return result;
    }

    // ── Step 04: Confirm COP transfer ─────────────────────────────────────

    /**
     * Records that the COP transfer has been received, advancing the
     * result to {@link TradeState#CONFIRMED}.
     *
     * @param pending result currently in PENDING state
     * @return new result in CONFIRMED state with COP amount populated
     */
    public TradeResult confirmTransfer(TradeResult pending) {
        Objects.requireNonNull(pending, "TradeResult cannot be null");
        double cop = fxRate.convert(pending.getOrder().getUsdtAmount());
        TradeResult confirmed = StateMachine.confirm(pending, cop);
        ledger.update(confirmed);
        auditLog.log(confirmed, "COP transfer confirmed: " + cop + " COP");
        return confirmed;
    }

    // ── Step 04: Release USDT ─────────────────────────────────────────────

    /**
     * Releases USDT to the buyer, advancing the result to
     * {@link TradeState#RELEASED}.
     *
     * <p>Must only be called after {@link #confirmTransfer}.
     *
     * @param confirmed result in CONFIRMED state
     * @return new result in RELEASED state
     * @throws InvalidStateTransitionException if not yet CONFIRMED
     */
    public TradeResult releaseUsdt(TradeResult confirmed) {
        Objects.requireNonNull(confirmed, "TradeResult cannot be null");
        TradeResult released = StateMachine.release(confirmed);
        ledger.update(released);
        auditLog.log(released, "USDT released to buyer");
        return released;
    }

    // ── Full execute shortcut ─────────────────────────────────────────────

    /**
     * Convenience method: lists the order, confirms the COP transfer, and
     * releases USDT in a single call (for testing happy paths).
     */
    public TradeResult execute(TradeOrder order) {
        TradeResult pending   = listSellOrder(order);
        TradeResult confirmed = confirmTransfer(pending);
        return releaseUsdt(confirmed);
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public ExchangeLedger getLedger()   { return ledger;   }
    public AuditLogger    getAuditLog() { return auditLog; }
    public FxRateProvider getFxRate()   { return fxRate;   }

    // ── Private helpers ───────────────────────────────────────────────────

    private void validateBuyer(Counterpart buyer) {
        if (buyer.getRating() < MIN_BUYER_RATING)
            throw new InsufficientTrustException(buyer.getUsername(),
                String.format("rating %.2f is below minimum %.2f",
                    buyer.getRating(), MIN_BUYER_RATING));

        if (buyer.getCompletedTrades() < MIN_BUYER_TRADES)
            throw new InsufficientTrustException(buyer.getUsername(),
                String.format("completed trades %d is below minimum %d",
                    buyer.getCompletedTrades(), MIN_BUYER_TRADES));
    }
}