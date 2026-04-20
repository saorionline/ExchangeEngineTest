package com.exchange.cop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * In-memory store that persists all {@link TradeResult} snapshots for the
 * current session.
 *
 * <p>Step 05–06 – the ledger is updated every time a result transitions
 * state so that the latest snapshot is always queryable.
 */
public final class ExchangeLedger {

    private final List<TradeResult> results = new ArrayList<>();

    /**
     * Appends a result to the ledger.
     *
     * @param result non-null trade result
     */
    public void add(TradeResult result) {
        if (result == null) throw new NullPointerException("TradeResult cannot be null");
        results.add(result);
    }

    /**
     * Replaces the latest entry for the same trade id with {@code updated}.
     * If no prior entry exists the result is simply appended.
     */
    public void update(TradeResult updated) {
        for (int i = results.size() - 1; i >= 0; i--) {
            if (results.get(i).getTradeId().equals(updated.getTradeId())) {
                results.set(i, updated);
                return;
            }
        }
        results.add(updated);
    }

    /** Returns the most recent result for the given trade id. */
    public Optional<TradeResult> findLatest(String tradeId) {
        for (int i = results.size() - 1; i >= 0; i--) {
            if (results.get(i).getTradeId().equals(tradeId))
                return Optional.of(results.get(i));
        }
        return Optional.empty();
    }

    /** Returns an unmodifiable snapshot of all recorded results. */
    public List<TradeResult> getAll() {
        return Collections.unmodifiableList(results);
    }

    public int size() { return results.size(); }

    public boolean isEmpty() { return results.isEmpty(); }

    /**
     * Writes a full audit log of every stored result to {@code logger}.
     */
    public void audit(AuditLogger logger) {
        results.forEach(r -> logger.log(r, "Ledger audit: " + r.getState()));
    }
}
