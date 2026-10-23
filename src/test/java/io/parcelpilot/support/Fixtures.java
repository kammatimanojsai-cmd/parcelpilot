package io.parcelpilot.support;

import io.parcelpilot.domain.EtaCalculator;
import io.parcelpilot.domain.HolidayCalendar;
import io.parcelpilot.domain.QuoteCalculator;
import io.parcelpilot.domain.RateCard;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Shared, deterministic test fixtures. */
public final class Fixtures {

    private Fixtures() {}

    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    /** Thursday 22 Oct 2026, 10:00 IST: a working day before every cut-off. */
    public static final LocalDateTime THURSDAY_10AM = LocalDateTime.of(2026, 10, 22, 10, 0);

    public static Clock thursdayMorning() {
        return Clock.fixed(Instant.parse("2026-10-22T04:30:00Z"), IST);
    }

    public static QuoteCalculator calculator() {
        return new QuoteCalculator(RateCard.standard(), new EtaCalculator(HolidayCalendar.fixedOnly()));
    }
}
