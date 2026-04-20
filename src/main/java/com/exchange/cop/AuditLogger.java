package com.exchange.cop;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Append-only event log for the exchange engine.
 *
 * <p>Step 06 – each state change is recorded as an immutable
 * {@link AuditEvent} so that the full lifecycle of every trade
 * can be replayed.
 */
public final class AuditLogger {

    private final List<AuditEvent> events = new ArrayList<>();

    public void log(TradeResult result, String description) {
        events.add(new AuditEvent(result.getTradeId(), result.getState(),
                                  description, Instant.now()));
    }

    /** Returns an unmodifiable view of the event log. */
    public List<AuditEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public int size() { return events.size(); }

    // ── inner class (Java 11 compatible) ──────────────────────────────────

    public static final class AuditEvent {
        private final String     tradeId;
        private final TradeState state;
        private final String     description;
        private final Instant    timestamp;

        public AuditEvent(String tradeId, TradeState state,
                          String description, Instant timestamp) {
            this.tradeId     = tradeId;
            this.state       = state;
            this.description = description;
            this.timestamp   = timestamp;
        }

        public String     tradeId()     { return tradeId;     }
        public TradeState state()       { return state;       }
        public String     description() { return description; }
        public Instant    timestamp()   { return timestamp;   }
    }
}
