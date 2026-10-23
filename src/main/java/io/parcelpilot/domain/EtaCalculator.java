package io.parcelpilot.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Estimates delivery. Bookings on a working day up to 14:00 are picked up the same day, later ones the next
 * working day. Same-day service must be booked by 11:00 on a working day.
 */
public final class EtaCalculator {

    public static final LocalTime PICKUP_CUTOFF = LocalTime.of(14, 0);
    public static final LocalTime SAME_DAY_CUTOFF = LocalTime.of(11, 0);

    private static final Map<Zone, Integer> STANDARD_DAYS = new EnumMap<>(Map.of(
            Zone.LOCAL, 1, Zone.STATE, 2, Zone.REGIONAL, 3, Zone.NATIONAL, 5, Zone.REMOTE, 7));
    private static final Map<Zone, Integer> EXPRESS_DAYS = new EnumMap<>(Map.of(
            Zone.LOCAL, 1, Zone.STATE, 1, Zone.REGIONAL, 2, Zone.NATIONAL, 3, Zone.REMOTE, 5));

    private final HolidayCalendar calendar;

    public EtaCalculator(HolidayCalendar calendar) {
        this.calendar = Objects.requireNonNull(calendar);
    }

    public static int transitDays(Zone zone, ServiceLevel service) {
        return switch (service) {
            case STANDARD -> STANDARD_DAYS.get(zone);
            case EXPRESS -> EXPRESS_DAYS.get(zone);
            case SAME_DAY -> 0;
        };
    }

    public Eta estimate(Zone zone, ServiceLevel service, LocalDateTime bookedAt) {
        LocalDate day = bookedAt.toLocalDate();
        LocalTime time = bookedAt.toLocalTime();
        if (service == ServiceLevel.SAME_DAY) {
            if (!calendar.isWorkingDay(day) || time.isAfter(SAME_DAY_CUTOFF)) {
                throw DomainException.rule("SAME_DAY_CUTOFF_PASSED", "Same-day bookings close at 11:00 on working days.");
            }
            return new Eta(day, day, 0);
        }
        LocalDate pickup = calendar.isWorkingDay(day) && !time.isAfter(PICKUP_CUTOFF) ? day : calendar.nextWorkingDay(day);
        int days = transitDays(zone, service);
        return new Eta(pickup, calendar.addWorkingDays(pickup, days), days);
    }
}
