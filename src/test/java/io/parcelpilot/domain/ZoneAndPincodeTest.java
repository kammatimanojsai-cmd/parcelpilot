package io.parcelpilot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PIN codes and zones (data-driven from zones.csv)")
class ZoneAndPincodeTest {

    @ParameterizedTest(name = "{0} → {1} is {2}: {3}")
    @CsvFileSource(resources = "/data/zones.csv", numLinesToSkip = 1)
    void resolvesZone(String origin, String destination, Zone expected, String why) {
        assertThat(Zone.between(Pincode.of(origin), Pincode.of(destination))).isEqualTo(expected);
    }

    @ParameterizedTest(name = "valid: \"{0}\"")
    @ValueSource(strings = {"110001", "999999", "100000", " 560001", "560001\t"})
    void acceptsValidPincodes(String raw) {
        assertThat(Pincode.of(raw).value()).isEqualTo(raw.strip());
    }

    @ParameterizedTest(name = "invalid: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"012345", "56001", "5600011", "56OO01", "560 01", "-56001", "５６０００１"})
    void rejectsInvalidPincodes(String raw) {
        assertThatThrownBy(() -> Pincode.of(raw))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo("INVALID_PINCODE");
    }

    @ParameterizedTest(name = "{0}xxx is remote")
    @ValueSource(strings = {"180001", "190001", "795001", "744101"})
    void remotePrefixes(String pin) {
        assertThat(Pincode.of(pin).remote()).isTrue();
    }

    @ParameterizedTest(name = "every zone has a label: {0}")
    @EnumSource(Zone.class)
    void zonesAreLabelled(Zone zone) {
        assertThat(zone.label()).isNotBlank();
    }

    @Test
    void printsAsTheDigits() {
        assertThat(Pincode.of("560001")).hasToString("560001");
    }
}
