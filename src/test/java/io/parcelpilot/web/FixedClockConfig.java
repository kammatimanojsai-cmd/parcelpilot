package io.parcelpilot.web;

import io.parcelpilot.support.Fixtures;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

/** Pins "now" to Thursday 22 Oct 2026, 10:00 IST so dates in API responses are predictable. */
@TestConfiguration
public class FixedClockConfig {

    @Bean
    @Primary
    Clock fixedClock() {
        return Fixtures.thursdayMorning();
    }
}
