package io.parcelpilot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Delivery estimates (data-driven from eta.csv)")
class EtaCalculatorTest {

    private final EtaCalculator eta = new EtaCalculator(HolidayCalendar.fixedOnly());

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/data/eta.csv", numLinesToSkip = 1)
    void estimates(String caseName, LocalDateTime bookedAt, Zone zone, ServiceLevel service,
                   LocalDate pickup, LocalDate delivery, int transitDays) {
        Eta e = eta.estimate(zone, service, bookedAt);

        assertThat(e).isEqualTo(new Eta(pickup, delivery, transitDays));
    }

    @ParameterizedTest(name = "{0} {1}: {2} working days")
    @CsvSource({
            "STANDARD, LOCAL, 1", "STANDARD, STATE, 2", "STANDARD, REGIONAL, 3", "STANDARD, NATIONAL, 5", "STANDARD, REMOTE, 7",
            "EXPRESS,  LOCAL, 1", "EXPRESS,  STATE, 1", "EXPRESS,  REGIONAL, 2", "EXPRESS,  NATIONAL, 3", "EXPRESS,  REMOTE, 5",
            "SAME_DAY, LOCAL, 0"
    })
    void transitTable(ServiceLevel service, Zone zone, int days) {
        assertThat(EtaCalculator.transitDays(zone, service)).isEqualTo(days);
    }

    @Test
    void configuredFestivalIsSkipped() {
        LocalDate festival = LocalDate.of(2026, 10, 23); // a Friday, configured as a holiday
        EtaCalculator withFestival = new EtaCalculator(new HolidayCalendar(Set.of(festival)));

        Eta e = withFestival.estimate(Zone.LOCAL, ServiceLevel.STANDARD, LocalDateTime.of(2026, 10, 22, 10, 0));

        assertThat(e.deliveryDate()).isEqualTo(LocalDate.of(2026, 10, 24));
    }

    @Test
    void negativeWorkingDaysAreRejected() {
        assertThatThrownBy(() -> HolidayCalendar.fixedOnly().addWorkingDays(LocalDate.of(2026, 1, 1), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
