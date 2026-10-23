package io.parcelpilot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Weights, slabs and money")
class ParcelAndMoneyTest {

    @ParameterizedTest(name = "{0} g, {1}×{2}×{3} cm → volumetric {4} g, billed {5} g in {6} slabs")
    @CsvSource(textBlock = """
            # weight, l,  w,  h, volumetric, chargeable, slabs
                   1,  1,  1,  1,          1,        500,     1
                 500, 10, 10, 10,        200,        500,     1
                 501, 10, 10, 10,        200,       1000,     2
                 100, 40, 30, 30,       7200,       7500,    15
                7200, 40, 30, 30,       7200,       7500,    15
                7501, 40, 30, 30,       7200,       8000,    16
                 999,  3,  3,  3,          6,       1000,     2
               50000,100, 50, 50,      50000,      50000,   100
            """)
    void chargeableWeight(int weight, int l, int w, int h, int volumetric, int chargeable, int slabs) {
        Parcel p = new Parcel(weight, l, w, h);

        assertThat(p.volumetricGrams()).isEqualTo(volumetric);
        assertThat(p.chargeableGrams()).isEqualTo(chargeable);
        assertThat(p.slabs()).isEqualTo(slabs);
        assertThat(p.volumetricApplies()).isEqualTo(volumetric > weight);
    }

    static Stream<Arguments> badParcels() {
        return Stream.of(
                arguments(0, 10, 10, 10, "WEIGHT_OUT_OF_RANGE"),
                arguments(-5, 10, 10, 10, "WEIGHT_OUT_OF_RANGE"),
                arguments(50_001, 10, 10, 10, "WEIGHT_OUT_OF_RANGE"),
                arguments(500, 0, 10, 10, "DIMENSION_OUT_OF_RANGE"),
                arguments(500, 10, 10, 151, "DIMENSION_OUT_OF_RANGE"),
                arguments(500, 150, 40, 36, "OVERSIZE"),
                arguments(500, 101, 50, 50, "OVERSIZE"));
    }

    @ParameterizedTest(name = "{0} g, {1}×{2}×{3} cm is rejected with {4}")
    @MethodSource("badParcels")
    void rejectsBadParcels(int weight, int l, int w, int h, String code) {
        assertThatThrownBy(() -> new Parcel(weight, l, w, h))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo(code);
    }

    @ParameterizedTest(name = "{1} bp of {0} paise = {2}")
    @CsvSource({
            "0,      1800, 0",
            "100,    1800, 18",
            "3920,   1800, 706",
            "250,    1800, 45",
            "25,     200,  1",
            "24,     200,  0",
            "300025, 200,  6001",
            "500001, 50,   2500"
    })
    void percentRoundsHalfUp(long amount, int bp, long expected) {
        assertThat(Money.percent(amount, bp)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} paise → {1}")
    @CsvSource(delimiter = '|', value = {
            "0          | ₹0.00",
            "5          | ₹0.05",
            "4626       | ₹46.26",
            "100000     | ₹1,000.00",
            "956838     | ₹9,568.38",
            "12345678   | ₹1,23,456.78",
            "1234567800 | ₹1,23,45,678.00",
            "-4626      | -₹46.26"
    })
    void formatsIndianRupees(long paise, String expected) {
        assertThat(Money.format(paise)).isEqualTo(expected);
    }

    @Test
    void percentOfNegativeIsRejected() {
        assertThatThrownBy(() -> Money.percent(-1, 100)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rateCardMustCoverEveryZone() {
        assertThatThrownBy(() -> new RateCard(Map.of(Zone.LOCAL, new RateCard.Rate(1, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("STATE");
    }

    @Test
    void freightNeedsAtLeastOneSlab() {
        assertThatThrownBy(() -> RateCard.standard().freightPaise(Zone.LOCAL, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(RateCard.standard().rate(Zone.REMOTE)).isEqualTo(new RateCard.Rate(9_000, 6_000));
    }

    @Test
    void quoteRefusesChargesThatDoNotAddUp() {
        var charges = java.util.List.of(new Quote.Charge(Quote.ChargeCode.FREIGHT, "x", 100));
        assertThatThrownBy(() -> new Quote(Zone.LOCAL, ServiceLevel.STANDARD, 1, 1, 500, charges, 99, null))
                .isInstanceOf(IllegalStateException.class);
    }
}
