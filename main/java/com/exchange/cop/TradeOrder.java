package com.exchange.cop;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable representation of a P2P sell order.
 *
 * <p>Constructed exclusively via the nested {@link Builder}.
 *
 * <pre>{@code
 * TradeOrder order = TradeOrder.builder()
 *     .usdtAmount(1500.00)
 *     .counterpart(buyer)
 *     .receiveVia(PaymentMethod.NEQUI)
 *     .phase(ExchangePhase.PILOT)
 *     .build();
 * }</pre>
 */
public final class TradeOrder {

    private final String        id;
    private final double        usdtAmount;
    private final Counterpart   counterpart;
    private final PaymentMethod receiveVia;
    private final ExchangePhase phase;

    private TradeOrder(Builder b) {
        this.id           = UUID.randomUUID().toString();
        this.usdtAmount   = b.usdtAmount;
        this.counterpart  = b.counterpart;
        this.receiveVia   = b.receiveVia;
        this.phase        = b.phase;
    }

    // ── factory ────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    // ── accessors ──────────────────────────────────────────────────────────

    public String        getId()          { return id;          }
    public double        getUsdtAmount()  { return usdtAmount;  }
    public Counterpart   getCounterpart() { return counterpart; }
    public PaymentMethod getReceiveVia()  { return receiveVia;  }
    public ExchangePhase getPhase()       { return phase;       }

    // ── toString ───────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "TradeOrder{id='%s', usdt=%.2f, buyer=%s, via=%s, phase=%s}",
            id, usdtAmount, counterpart.getUsername(), receiveVia, phase);
    }

    // ── Builder ────────────────────────────────────────────────────────────

    public static final class Builder {

        private double        usdtAmount;
        private Counterpart   counterpart;
        private PaymentMethod receiveVia;
        private ExchangePhase phase;

        private Builder() {}

        /**
         * @param amount USDT amount to sell (must be &gt; 0)
         */
        public Builder usdtAmount(double amount) {
            if (amount <= 0)
                throw new IllegalArgumentException(
                    "USDT amount must be greater than 0, was: " + amount);
            this.usdtAmount = amount;
            return this;
        }

        /**
         * @param counterpart Binance P2P buyer profile (non-null)
         */
        public Builder counterpart(Counterpart counterpart) {
            this.counterpart = Objects.requireNonNull(counterpart,
                "Counterpart (buyer) cannot be null");
            return this;
        }

        /**
         * @param method Payment method for receiving COP (non-null)
         */
        public Builder receiveVia(PaymentMethod method) {
            this.receiveVia = Objects.requireNonNull(method,
                "Payment method cannot be null");
            return this;
        }

        /**
         * @param phase Exchange phase (PILOT or SCALING) (non-null)
         */
        public Builder phase(ExchangePhase phase) {
            this.phase = Objects.requireNonNull(phase,
                "Exchange phase cannot be null");
            return this;
        }

        /**
         * Builds and validates the {@link TradeOrder}.
         *
         * @throws IllegalStateException if any required field is missing
         */
        public TradeOrder build() {
            if (usdtAmount <= 0)
                throw new IllegalStateException("usdtAmount must be set and > 0");
            Objects.requireNonNull(counterpart, "counterpart must be set");
            Objects.requireNonNull(receiveVia,  "receiveVia must be set");
            Objects.requireNonNull(phase,       "phase must be set");
            return new TradeOrder(this);
        }
    }
}