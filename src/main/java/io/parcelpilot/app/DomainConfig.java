package io.parcelpilot.app;

import io.parcelpilot.domain.EtaCalculator;
import io.parcelpilot.domain.HolidayCalendar;
import io.parcelpilot.domain.QuoteCalculator;
import io.parcelpilot.domain.RateCard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.random.RandomGenerator;

/** Wires the framework-free domain into Spring. Time and randomness are beans so tests can pin them. */
@Configuration
public class DomainConfig {

    @Bean
    Clock clock(@Value("${parcelpilot.time-zone:Asia/Kolkata}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    HolidayCalendar holidayCalendar(@Value("${parcelpilot.extra-holidays:}") List<String> extra) {
        return new HolidayCalendar(extra.stream().map(String::strip).filter(s -> !s.isEmpty()).map(LocalDate::parse).toList());
    }

    @Bean
    EtaCalculator etaCalculator(HolidayCalendar calendar) {
        return new EtaCalculator(calendar);
    }

    @Bean
    QuoteCalculator quoteCalculator(EtaCalculator eta) {
        return new QuoteCalculator(RateCard.standard(), eta);
    }

    @Bean
    RandomGenerator trackingRandom() {
        return new SecureRandom();
    }
}
