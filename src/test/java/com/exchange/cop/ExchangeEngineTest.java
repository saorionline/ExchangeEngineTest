package com.exchange.cop;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for <strong>Step 01 – Sell USDT on Binance P2P</strong>.
 *
 * <p>Covers three areas:
 * <ol>
 *   <li><em>Counterpart creation</em> – valid profiles, boundary values,
 *       and all invalid-input paths.</li>
 *   <li><em>TradeOrder building</em> – builder happy path, null safety,
 *       and amount validation.</li>
 *   <li><em>P2P listing via ExchangeEngine</em> – reputable buyer accepted,
 *       low-rating / low-trade buyer rejected, ledger state after listing,
 *       and the confirm-before-release safety invariant.</li>
 * </ol>
 */
@DisplayName("Step 01 · Sell USDT on Binance P2P")
class ExchangeEngineTest {

    // ── shared fixtures ───────────────────────────────────────────────────

    /** A reputable buyer that satisfies all engine thresholds. */
    private static final Counterpart REPUTABLE_BUYER =
        Counterpart.of("carlos_btc_co", 4.95, 312);

    /** Exact minimum rating allowed by the engine. */
    private static final Counterpart BUYER_AT_MIN_RATING =
        Counterpart.of("min_rating_buyer", ExchangeEngine.MIN_BUYER_RATING,
                       ExchangeEngine.MIN_BUYER_TRADES);

    /** Rating just below the threshold. */
    private static final Counterpart LOW_RATING_BUYER =
        Counterpart.of("low_rating_buyer", 4.4, 200);

    /** Too few completed trades. */
    private static final Counterpart FEW_TRADES_BUYER =
        Counterpart.of("newbie_buyer", 4.9, 10);

    private ExchangeEngine engine;

    @BeforeEach
    void setUp() {
        engine = ExchangeEngine.create();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 1. Counterpart creation
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1 · Counterpart profile")
    class CounterpartTests {

        @Test
        @DisplayName("Valid profile — all fields stored correctly")
        void givenValidProfile_whenCreated_thenFieldsMatch() {
            Counterpart cp = Counterpart.of("carlos_btc_co", 4.95, 312);

            assertAll("counterpart fields",
                () -> assertEquals("carlos_btc_co", cp.getUsername()),
                () -> assertEquals(4.95, cp.getRating(), 0.001),
                () -> assertEquals(312,  cp.getCompletedTrades()),
                () -> assertEquals(95.0, cp.getCompletionRate(), 0.001)
            );
        }

        @Test
        @DisplayName("Explicit completion rate — stored correctly")
        void givenExplicitCompletionRate_whenCreated_thenRateStored() {
            Counterpart cp = Counterpart.of("trader_x", 4.8, 100, 98.5);
            assertEquals(98.5, cp.getCompletionRate(), 0.001);
        }

        @Test
        @DisplayName("Null username — throws NullPointerException")
        void givenNullUsername_whenCreated_thenNPE() {
            assertThrows(NullPointerException.class,
                () -> Counterpart.of(null, 4.9, 100));
        }

        @Test
        @DisplayName("Blank username — throws IllegalArgumentException")
        void givenBlankUsername_whenCreated_thenIllegalArgument() {
            assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                    () -> Counterpart.of("", 4.9, 100)),
                () -> assertThrows(IllegalArgumentException.class,
                    () -> Counterpart.of("   ", 4.9, 100))
            );
        }

        @ParameterizedTest(name = "rating={0} is out of range")
        @ValueSource(doubles = {-0.1, 5.1, -5.0, 10.0})
        @DisplayName("Out-of-range rating — throws IllegalArgumentException")
        void givenOutOfRangeRating_whenCreated_thenIllegalArgument(double rating) {
            assertThrows(IllegalArgumentException.class,
                () -> Counterpart.of("user", rating, 100));
        }

        @ParameterizedTest(name = "trades={0} is valid boundary")
        @ValueSource(ints = {0, 1, 50, 1000})
        @DisplayName("Zero or positive trade count — accepted")
        void givenNonNegativeTradeCount_whenCreated_thenNoException(int trades) {
            assertDoesNotThrow(() -> Counterpart.of("user", 4.5, trades));
        }

        @Test
        @DisplayName("Negative trade count — throws IllegalArgumentException")
        void givenNegativeTradeCount_whenCreated_thenIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                () -> Counterpart.of("user", 4.8, -1));
        }

        @Test
        @DisplayName("Boundary ratings 0.0 and 5.0 — accepted")
        void givenBoundaryRatings_whenCreated_thenNoException() {
            assertAll(
                () -> assertDoesNotThrow(() -> Counterpart.of("u1", 0.0, 0)),
                () -> assertDoesNotThrow(() -> Counterpart.of("u2", 5.0, 0))
            );
        }

        @Test
        @DisplayName("Out-of-range completion rate — throws IllegalArgumentException")
        void givenOutOfRangeCompletionRate_whenCreated_thenIllegalArgument() {
            assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                    () -> Counterpart.of("user", 4.8, 100, -1.0)),
                () -> assertThrows(IllegalArgumentException.class,
                    () -> Counterpart.of("user", 4.8, 100, 100.1))
            );
        }

        @Test
        @DisplayName("Equality based on username")
        void givenSameUsername_whenCompared_thenEqual() {
            Counterpart a = Counterpart.of("carlos_btc_co", 4.9, 300);
            Counterpart b = Counterpart.of("carlos_btc_co", 4.5, 100);
            assertEquals(a, b);
        }

        @Test
        @DisplayName("toString contains username and rating")
        void givenCounterpart_whenToString_thenContainsKeyInfo() {
            Counterpart cp = Counterpart.of("trader_z", 4.75, 80);
            String s = cp.toString();
            assertTrue(s.contains("trader_z"), "toString should contain username");
            assertTrue(s.contains("4.75") || s.contains("4,75"),
                "toString should contain rating");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. TradeOrder builder
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2 · TradeOrder builder")
    class TradeOrderBuilderTests {

        @Test
        @DisplayName("All valid fields — order built with correct values")
        void givenAllValidFields_whenBuilt_thenFieldsMatch() {
            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_500.00)
                .counterpart(REPUTABLE_BUYER)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();

            assertAll("order fields",
                () -> assertNotNull(order.getId(),         "id must be auto-generated"),
                () -> assertEquals(1_500.00, order.getUsdtAmount(), 0.001),
                () -> assertEquals(REPUTABLE_BUYER, order.getCounterpart()),
                () -> assertEquals(PaymentMethod.NEQUI,   order.getReceiveVia()),
                () -> assertEquals(ExchangePhase.PILOT,   order.getPhase())
            );
        }

        @Test
        @DisplayName("Each order gets a unique id")
        void givenTwoOrders_whenBuilt_thenDifferentIds() {
            TradeOrder o1 = TradeOrder.builder()
                .usdtAmount(1_000).counterpart(REPUTABLE_BUYER)
                .receiveVia(PaymentMethod.NEQUI).phase(ExchangePhase.PILOT).build();

            TradeOrder o2 = TradeOrder.builder()
                .usdtAmount(2_000).counterpart(REPUTABLE_BUYER)
                .receiveVia(PaymentMethod.BANK_TRANSFER).phase(ExchangePhase.SCALING).build();

            assertNotEquals(o1.getId(), o2.getId(), "Order IDs must be unique");
        }

        @Test
        @DisplayName("Null counterpart — throws NullPointerException")
        void givenNullCounterpart_whenBuilt_thenNPE() {
            assertThrows(NullPointerException.class, () ->
                TradeOrder.builder()
                    .usdtAmount(1_000)
                    .counterpart(null)       // ← should throw
                    .receiveVia(PaymentMethod.NEQUI)
                    .phase(ExchangePhase.PILOT)
                    .build());
        }

        @Test
        @DisplayName("Null payment method — throws NullPointerException")
        void givenNullPaymentMethod_whenBuilt_thenNPE() {
            assertThrows(NullPointerException.class, () ->
                TradeOrder.builder()
                    .usdtAmount(1_000)
                    .counterpart(REPUTABLE_BUYER)
                    .receiveVia(null)        // ← should throw
                    .phase(ExchangePhase.PILOT)
                    .build());
        }

        @Test
        @DisplayName("Null phase — throws NullPointerException")
        void givenNullPhase_whenBuilt_thenNPE() {
            assertThrows(NullPointerException.class, () ->
                TradeOrder.builder()
                    .usdtAmount(1_000)
                    .counterpart(REPUTABLE_BUYER)
                    .receiveVia(PaymentMethod.NEQUI)
                    .phase(null)             // ← should throw
                    .build());
        }

        @ParameterizedTest(name = "amount={0} is invalid")
        @ValueSource(doubles = {0.0, -1.0, -500.0})
        @DisplayName("Zero or negative USDT amount — throws IllegalArgumentException")
        void givenInvalidAmount_whenBuilt_thenIllegalArgument(double amount) {
            assertThrows(IllegalArgumentException.class, () ->
                TradeOrder.builder().usdtAmount(amount));
        }

        @Test
        @DisplayName("Build without counterpart — throws IllegalStateException")
        void givenMissingCounterpart_whenBuild_thenIllegalState() {
            assertThrows(Exception.class, () ->
                TradeOrder.builder()
                    .usdtAmount(1_000)
                    .receiveVia(PaymentMethod.NEQUI)
                    .phase(ExchangePhase.PILOT)
                    .build());   // counterpart not set
        }

        @Test
        @DisplayName("BANK_TRANSFER payment method — stored correctly")
        void givenBankTransfer_whenBuilt_thenMethodIsBank() {
            TradeOrder order = TradeOrder.builder()
                .usdtAmount(2_000)
                .counterpart(REPUTABLE_BUYER)
                .receiveVia(PaymentMethod.BANK_TRANSFER)
                .phase(ExchangePhase.SCALING)
                .build();
            assertEquals(PaymentMethod.BANK_TRANSFER, order.getReceiveVia());
        }

        @Test
        @DisplayName("toString contains USDT amount and buyer username")
        void givenOrder_whenToString_thenContainsKeyInfo() {
            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_500)
                .counterpart(REPUTABLE_BUYER)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();
            String s = order.toString();
            assertTrue(s.contains("1500") || s.contains("1,500"),
                "toString should contain USDT amount");
            assertTrue(s.contains("carlos_btc_co"),
                "toString should contain buyer username");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. Engine creation
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3 · Engine initialisation")
    class EngineInitTests {

        @Test
        @DisplayName("create() returns non-null engine")
        void whenCreated_thenEngineIsNotNull() {
            assertNotNull(ExchangeEngine.create());
        }

        @Test
        @DisplayName("Fresh engine has empty ledger")
        void whenCreated_thenLedgerIsEmpty() {
            ExchangeEngine fresh = ExchangeEngine.create();
            assertTrue(fresh.getLedger().isEmpty(), "Ledger must be empty on creation");
        }

        @Test
        @DisplayName("Ledger reference is non-null")
        void whenCreated_thenLedgerIsNotNull() {
            assertNotNull(engine.getLedger());
        }

        @Test
        @DisplayName("create(FxRateProvider) with null — throws NullPointerException")
        void givenNullFxRate_whenCreated_thenNPE() {
            assertThrows(NullPointerException.class,
                () -> ExchangeEngine.create(null));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. listSellOrder — happy path
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("4 · listSellOrder — accepted orders")
    class ListSellOrderHappyPathTests {

        private TradeOrder pilotOrder;

        @BeforeEach
        void buildOrder() {
            pilotOrder = TradeOrder.builder()
                .usdtAmount(1_500.00)
                .counterpart(REPUTABLE_BUYER)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();
        }

        @Test
        @DisplayName("Reputable buyer — result is non-null")
        void givenReputableBuyer_whenListed_thenResultNotNull() {
            assertNotNull(engine.listSellOrder(pilotOrder));
        }

        @Test
        @DisplayName("Reputable buyer — initial state is PENDING")
        void givenReputableBuyer_whenListed_thenStateIsPending() {
            TradeResult result = engine.listSellOrder(pilotOrder);
            assertEquals(TradeState.PENDING, result.getState());
        }

        @Test
        @DisplayName("Reputable buyer — COP received is 0 while PENDING")
        void givenReputableBuyer_whenListed_thenCopReceivedIsZero() {
            TradeResult result = engine.listSellOrder(pilotOrder);
            assertEquals(0.0, result.getCopReceived(), 0.001);
        }

        @Test
        @DisplayName("Reputable buyer — order added to ledger")
        void givenReputableBuyer_whenListed_thenLedgerHasOneEntry() {
            engine.listSellOrder(pilotOrder);
            assertEquals(1, engine.getLedger().size());
        }

        @Test
        @DisplayName("Multiple orders — all entries present in ledger")
        void givenTwoOrders_whenBothListed_thenLedgerHasTwoEntries() {
            TradeOrder order2 = TradeOrder.builder()
                .usdtAmount(2_000.00)
                .counterpart(REPUTABLE_BUYER)
                .receiveVia(PaymentMethod.BANK_TRANSFER)
                .phase(ExchangePhase.SCALING)
                .build();

            engine.listSellOrder(pilotOrder);
            engine.listSellOrder(order2);

            assertEquals(2, engine.getLedger().size());
        }

        @Test
        @DisplayName("Result trade id matches the original order id")
        void givenOrder_whenListed_thenResultIdMatchesOrderId() {
            TradeResult result = engine.listSellOrder(pilotOrder);
            assertEquals(pilotOrder.getId(), result.getTradeId());
        }

        @Test
        @DisplayName("Buyer at exact minimum rating — order accepted")
        void givenBuyerAtMinRating_whenListed_thenNoException() {
            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_000.00)
                .counterpart(BUYER_AT_MIN_RATING)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();

            assertDoesNotThrow(() -> engine.listSellOrder(order));
        }

        @Test
        @DisplayName("Audit log updated after listing")
        void givenOrder_whenListed_thenAuditLogIsNotEmpty() {
            engine.listSellOrder(pilotOrder);
            assertFalse(engine.getAuditLog().getEvents().isEmpty(),
                "Audit log should contain the listing event");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. listSellOrder — trust guard rejections
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("5 · listSellOrder — rejected buyers")
    class ListSellOrderRejectionTests {

        @Test
        @DisplayName("Rating below minimum — throws InsufficientTrustException")
        void givenLowRatingBuyer_whenListed_thenInsufficientTrust() {
            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_000.00)
                .counterpart(LOW_RATING_BUYER)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();

            InsufficientTrustException ex =
                assertThrows(InsufficientTrustException.class,
                    () -> engine.listSellOrder(order));

            assertEquals("low_rating_buyer", ex.getBuyerUsername());
        }

        @Test
        @DisplayName("Too few trades — throws InsufficientTrustException")
        void givenInexperiencedBuyer_whenListed_thenInsufficientTrust() {
            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_000.00)
                .counterpart(FEW_TRADES_BUYER)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();

            assertThrows(InsufficientTrustException.class,
                () -> engine.listSellOrder(order));
        }

        @Test
        @DisplayName("Rejected order — ledger stays empty")
        void givenRejectedOrder_whenListed_thenLedgerUnchanged() {
            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_000.00)
                .counterpart(LOW_RATING_BUYER)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();

            assertThrows(InsufficientTrustException.class,
                () -> engine.listSellOrder(order));

            assertTrue(engine.getLedger().isEmpty(),
                "Ledger must remain empty after rejected listing");
        }

        @Test
        @DisplayName("Rating one step below threshold — boundary rejection")
        void givenRatingJustBelowMinimum_whenListed_thenRejected() {
            double justBelow = ExchangeEngine.MIN_BUYER_RATING - 0.01;
            Counterpart borderBuyer =
                Counterpart.of("border_buyer", justBelow, ExchangeEngine.MIN_BUYER_TRADES + 10);

            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_000.00)
                .counterpart(borderBuyer)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();

            assertThrows(InsufficientTrustException.class,
                () -> engine.listSellOrder(order));
        }

        @Test
        @DisplayName("Trades one step below threshold — boundary rejection")
        void givenTradesJustBelowMinimum_whenListed_thenRejected() {
            Counterpart borderBuyer =
                Counterpart.of("border_buyer2",
                    ExchangeEngine.MIN_BUYER_RATING,
                    ExchangeEngine.MIN_BUYER_TRADES - 1);

            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_000.00)
                .counterpart(borderBuyer)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();

            assertThrows(InsufficientTrustException.class,
                () -> engine.listSellOrder(order));
        }

        @Test
        @DisplayName("Null order — throws NullPointerException")
        void givenNullOrder_whenListed_thenNPE() {
            assertThrows(NullPointerException.class,
                () -> engine.listSellOrder(null));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 6. Safety invariant — confirm before release
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("6 · Safety invariant · confirm before release")
    class SafetyInvariantTests {

        @Test
        @DisplayName("Release USDT without confirming — throws InvalidStateTransitionException")
        void givenPendingResult_whenReleased_thenInvalidStateTransition() {
            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_500.00)
                .counterpart(REPUTABLE_BUYER)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();

            TradeResult pending = engine.listSellOrder(order);

            assertThrows(InvalidStateTransitionException.class,
                () -> engine.releaseUsdt(pending),
                "Should not release USDT from PENDING state");
        }

        @Test
        @DisplayName("Confirm then release — reaches RELEASED state")
        void givenPendingResult_whenConfirmedThenReleased_thenStateIsReleased() {
            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_500.00)
                .counterpart(REPUTABLE_BUYER)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();

            TradeResult pending   = engine.listSellOrder(order);
            TradeResult confirmed = engine.confirmTransfer(pending);
            TradeResult released  = engine.releaseUsdt(confirmed);

            assertEquals(TradeState.RELEASED, released.getState());
        }

        @Test
        @DisplayName("Confirm sets non-zero COP amount")
        void givenPendingResult_whenConfirmed_thenCopAmountPositive() {
            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_500.00)
                .counterpart(REPUTABLE_BUYER)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();

            TradeResult confirmed = engine.confirmTransfer(engine.listSellOrder(order));

            assertTrue(confirmed.getCopReceived() > 0,
                "COP amount must be positive after confirmation");
        }

        @Test
        @DisplayName("InvalidStateTransitionException carries correct from/to states")
        void givenPendingResult_whenReleased_thenExceptionHasCorrectStates() {
            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_000.00)
                .counterpart(REPUTABLE_BUYER)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();

            TradeResult pending = engine.listSellOrder(order);

            InvalidStateTransitionException ex =
                assertThrows(InvalidStateTransitionException.class,
                    () -> engine.releaseUsdt(pending));

            assertAll("exception states",
                () -> assertEquals(TradeState.PENDING,  ex.getFrom()),
                () -> assertEquals(TradeState.RELEASED, ex.getTo())
            );
        }

        @Test
        @DisplayName("execute() convenience method reaches RELEASED in one call")
        void givenReputableBuyer_whenExecuted_thenStateIsReleased() {
            TradeOrder order = TradeOrder.builder()
                .usdtAmount(1_000.00)
                .counterpart(REPUTABLE_BUYER)
                .receiveVia(PaymentMethod.NEQUI)
                .phase(ExchangePhase.PILOT)
                .build();

            TradeResult result = engine.execute(order);

            assertEquals(TradeState.RELEASED, result.getState());
        }
    }
}