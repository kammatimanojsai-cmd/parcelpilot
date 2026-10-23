package io.parcelpilot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.SplittableRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tracking numbers")
class TrackingIdTest {

    @ParameterizedTest(name = "check digit for {0} is {1}")
    @CsvSource({"000000000, 0", "123456789, 7", "482193706, 9", "999999999, 9", "000000001, 8"})
    void checkDigit(String payload, int digit) {
        assertThat(TrackingId.checkDigit(payload)).isEqualTo(digit);
        assertThat(TrackingId.isValid("PP" + payload + digit)).isTrue();
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"PP1234567890", "PP123456789", "PP12345678977", "XX1234567897", "pp12345678 97", "PP12345678A7"})
    void rejectsMalformedOrBadCheckDigit(String raw) {
        assertThat(TrackingId.isValid(raw)).isFalse();
        assertThatThrownBy(() -> TrackingId.parse(raw)).isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo("INVALID_TRACKING_ID");
    }

    @ParameterizedTest(name = "normalises \"{0}\"")
    @ValueSource(strings = {"pp1234567897", " PP1234567897 ", "Pp1234567897"})
    void parsingIsForgiving(String raw) {
        assertThat(TrackingId.parse(raw).value()).isEqualTo("PP1234567897");
    }

    @RepeatedTest(value = 25, name = "generated id {currentRepetition}/{totalRepetitions} is valid")
    void generatedIdsAreValid() {
        TrackingId id = TrackingId.generate(new SplittableRandom());
        assertThat(TrackingId.isValid(id.value())).as(id.value()).isTrue();
        assertThat(id).hasToString(id.value());
    }

    /** Luhn catches every single-digit typo: change each digit of a real id to each other value. */
    @TestFactory
    Stream<DynamicTest> everySingleDigitTypoIsCaught() {
        String id = TrackingId.generate(new SplittableRandom(7)).value();
        return IntStream.range(2, id.length()).boxed().flatMap(pos -> IntStream.rangeClosed(0, 9)
                .filter(d -> d != id.charAt(pos) - '0')
                .mapToObj(d -> {
                    String typo = id.substring(0, pos) + d + id.substring(pos + 1);
                    return DynamicTest.dynamicTest("position " + pos + " → " + d + " (" + typo + ")",
                            () -> assertThat(TrackingId.isValid(typo)).isFalse());
                }));
    }
}
