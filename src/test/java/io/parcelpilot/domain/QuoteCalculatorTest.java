package io.parcelpilot.domain;

import io.parcelpilot.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.EnumSource;

import static io.parcelpilot.domain.Quote.ChargeCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every row of {@code quotes.csv} is a full pricing case: the request in the first ten columns and the expected
 * itemised bill (in paise) in the rest. The expected values were produced by an independent implementation of the
 * rate rules using decimal arithmetic.
 */
@DisplayName("Quote pricing (data-driven from quotes.csv)")
class QuoteCalculatorTest {

    private final QuoteCalculator calculator = Fixtures.calculator();

    /** Builds the domain request from columns 1-9 of a CSV row. */
    static class RequestAggregator implements ArgumentsAggregator {
        @Override
        public QuoteRequest aggregateArguments(ArgumentsAccessor row, ParameterContext context) {
            return new QuoteRequest(
                    Pincode.of(row.getString(1)), Pincode.of(row.getString(2)),
                    new Parcel(row.getInteger(3), row.getInteger(4), row.getInteger(5), row.getInteger(6)),
                    row.get(7, ServiceLevel.class), row.getBoolean(8), row.getLong(9));
        }
    }

    record Expected(Zone zone, int volumetricGrams, int chargeableGrams, long freight, long fuel, long codFee,
                    long insurance, long gst, long total) {}

    /** Reads the expected bill from columns 10-18. */
    static class ExpectedAggregator implements ArgumentsAggregator {
        @Override
        public Expected aggregateArguments(ArgumentsAccessor row, ParameterContext context) {
            return new Expected(row.get(10, Zone.class), row.getInteger(11), row.getInteger(12), row.getLong(13),
                    row.getLong(14), row.getLong(15), row.getLong(16), row.getLong(17), row.getLong(18));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/data/quotes.csv", numLinesToSkip = 1)
    void pricesEachCaseExactly(String caseName,
                               @AggregateWith(RequestAggregator.class) QuoteRequest request,
                               @AggregateWith(ExpectedAggregator.class) Expected expected) {
        Quote q = calculator.quote(request, Fixtures.THURSDAY_10AM);

        assertAll(caseName,
                () -> assertEquals(expected.zone(), q.zone(), "zone"),
                () -> assertEquals(expected.volumetricGrams(), q.volumetricGrams(), "volumetric weight"),
                () -> assertEquals(expected.chargeableGrams(), q.chargeableGrams(), "chargeable weight"),
                () -> assertEquals(expected.freight(), q.amount(FREIGHT), "freight"),
                () -> assertEquals(expected.fuel(), q.amount(FUEL_SURCHARGE), "fuel surcharge"),
                () -> assertEquals(expected.codFee(), q.amount(COD_FEE), "COD fee"),
                () -> assertEquals(expected.insurance(), q.amount(INSURANCE), "insurance"),
                () -> assertEquals(expected.gst(), q.amount(GST), "GST"),
                () -> assertEquals(expected.total(), q.totalPaise(), "total"));
    }

    @ParameterizedTest(name = "{0}: optional lines appear only when charged")
    @CsvFileSource(resources = "/data/quotes.csv", numLinesToSkip = 1)
    void optionalLinesOnlyWhenNonZero(String caseName, @AggregateWith(RequestAggregator.class) QuoteRequest request) {
        Quote q = calculator.quote(request, Fixtures.THURSDAY_10AM);

        assertThat(q.charges()).allSatisfy(c -> assertThat(c.amountPaise()).isPositive());
        assertThat(q.charges().get(0).code()).isEqualTo(FREIGHT);
        assertThat(q.charges().get(q.charges().size() - 1).code()).isEqualTo(GST);
        assertThat(q.charges().stream().anyMatch(c -> c.code() == COD_FEE)).isEqualTo(request.cashOnDelivery());
    }

    @ParameterizedTest(name = "{0} costs at least as much as STANDARD")
    @EnumSource(value = ServiceLevel.class, names = "SAME_DAY", mode = EnumSource.Mode.EXCLUDE)
    void fasterServiceNeverCostsLess(ServiceLevel service) {
        Parcel parcel = new Parcel(2_000, 30, 20, 10);
        QuoteRequest standard = new QuoteRequest(Pincode.of("110001"), Pincode.of("400001"), parcel, ServiceLevel.STANDARD, false, 0);
        QuoteRequest other = new QuoteRequest(Pincode.of("110001"), Pincode.of("400001"), parcel, service, false, 0);

        assertThat(calculator.quote(other, Fixtures.THURSDAY_10AM).totalPaise())
                .isGreaterThanOrEqualTo(calculator.quote(standard, Fixtures.THURSDAY_10AM).totalPaise());
    }

    @Test
    void freightLabelDescribesTheBill() {
        QuoteRequest r = new QuoteRequest(Pincode.of("500001"), Pincode.of("500032"), new Parcel(1_200, 10, 10, 10),
                ServiceLevel.EXPRESS, false, 0);

        assertThat(calculator.quote(r, Fixtures.THURSDAY_10AM).charges().get(0).label())
                .isEqualTo("Express freight, same city, 1.5 kg billed");
    }

    @Test
    void wholeKilogramsAreLabelledWithoutDecimals() {
        QuoteRequest r = new QuoteRequest(Pincode.of("500001"), Pincode.of("500032"), new Parcel(2_000, 10, 10, 10),
                ServiceLevel.STANDARD, false, 0);

        assertThat(calculator.quote(r, Fixtures.THURSDAY_10AM).charges().get(0).label()).endsWith(", 2 kg billed");
    }
}
