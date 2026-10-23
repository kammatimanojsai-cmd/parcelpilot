package io.parcelpilot.domain;

import io.parcelpilot.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Shipment lifecycle (data-driven from transitions.csv and lifecycle.csv)")
class ShipmentLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-10-22T04:30:00Z");

    /** Reads the expected transition table, then checks all 8 × 8 status pairs against the code. */
    @TestFactory
    Stream<DynamicContainer> transitionMatrix() throws Exception {
        Map<ShipmentStatus, Set<ShipmentStatus>> expected = readTransitions();
        assertThat(expected.keySet()).as("transitions.csv lists every status").containsExactlyInAnyOrder(ShipmentStatus.values());

        return Arrays.stream(ShipmentStatus.values()).map(from -> DynamicContainer.dynamicContainer("from " + from,
                Arrays.stream(ShipmentStatus.values()).map(to -> {
                    boolean allowed = expected.get(from).contains(to);
                    return DynamicTest.dynamicTest((allowed ? "may move to " : "may not move to ") + to,
                            () -> assertThat(from.canMoveTo(to)).isEqualTo(allowed));
                })));
    }

    @Test
    void terminalStatusesHaveNoNextStep() throws Exception {
        Map<ShipmentStatus, Set<ShipmentStatus>> expected = readTransitions();
        for (ShipmentStatus s : ShipmentStatus.values()) {
            assertThat(s.terminal()).as(s.name()).isEqualTo(expected.get(s).isEmpty());
            assertThat(s.next()).as(s.name()).isEqualTo(expected.get(s));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/data/lifecycle.csv", numLinesToSkip = 1)
    void playsScenario(String scenario, String steps, ShipmentStatus expectedFinal, String expectedError, int failedAttempts) {
        Shipment s = newShipment();
        String[] moves = steps.split(">");
        for (int i = 0; i < moves.length; i++) {
            ShipmentStatus target = ShipmentStatus.valueOf(moves[i]);
            boolean last = i == moves.length - 1;
            if (last && expectedError != null) {
                DomainException e = assertThrows(DomainException.class, () -> s.advance(target, "Hub", null, NOW));
                assertThat(e.code()).isEqualTo(expectedError);
                assertThat(e.kind()).isEqualTo(DomainException.Kind.CONFLICT);
            } else {
                s.advance(target, "Hub", null, NOW);
            }
        }
        assertThat(s.status()).isEqualTo(expectedFinal);
        assertThat(s.failedAttempts()).isEqualTo(failedAttempts);
        int applied = expectedError == null ? moves.length : moves.length - 1;
        assertThat(s.events()).hasSize(1 + applied);
    }

    @Test
    void bookingRecordsTheFirstEvent() {
        Shipment s = newShipment();

        assertThat(s.status()).isEqualTo(ShipmentStatus.BOOKED);
        assertThat(s.events()).singleElement().satisfies(e -> {
            assertThat(e.status()).isEqualTo(ShipmentStatus.BOOKED);
            assertThat(e.location()).isEqualTo("560001");
            assertThat(e.at()).isEqualTo(NOW);
        });
        assertThat(s.receiverName()).isEqualTo("Asha");
        assertThat(s.quote().totalPaise()).isPositive();
        assertThat(s.request().destination().value()).isEqualTo("110001");
    }

    private static Shipment newShipment() {
        QuoteRequest r = new QuoteRequest(Pincode.of("560001"), Pincode.of("110001"), new Parcel(1_000, 20, 20, 10),
                ServiceLevel.STANDARD, false, 0);
        Quote q = Fixtures.calculator().quote(r, Fixtures.THURSDAY_10AM);
        return Shipment.book(TrackingId.parse("PP1234567897"), r, q, "Asha", NOW);
    }

    private static Map<ShipmentStatus, Set<ShipmentStatus>> readTransitions() throws Exception {
        Map<ShipmentStatus, Set<ShipmentStatus>> table = new EnumMap<>(ShipmentStatus.class);
        try (var in = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(ShipmentLifecycleTest.class.getResourceAsStream("/data/transitions.csv")), StandardCharsets.UTF_8))) {
            in.lines().skip(1).filter(l -> !l.isBlank()).forEach(line -> {
                String[] cols = line.split(",", -1);
                Set<ShipmentStatus> next = Arrays.stream(cols[1].trim().split("\\s+")).filter(s -> !s.isEmpty())
                        .map(ShipmentStatus::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(ShipmentStatus.class)));
                table.put(ShipmentStatus.valueOf(cols[0].trim()), next);
            });
        }
        return table;
    }
}
