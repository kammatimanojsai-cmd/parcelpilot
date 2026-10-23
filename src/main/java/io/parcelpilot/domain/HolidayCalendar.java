package io.parcelpilot.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Collection;
import java.util.Set;

/**
 * Working days for pickups and deliveries: Monday to Saturday, except fixed-date national holidays
 * (26 Jan, 15 Aug, 2 Oct, 25 Dec) and any extra dates configured for festivals that move each year.
 */
public final class HolidayCalendar {

    public static final Set<MonthDay> FIXED_HOLIDAYS = Set.of(
            MonthDay.of(1, 26), MonthDay.of(8, 15), MonthDay.of(10, 2), MonthDay.of(12, 25));

    private final Set<LocalDate> extra;

    public HolidayCalendar(Collection<LocalDate> extraHolidays) {
        this.extra = Set.copyOf(extraHolidays);
    }

    public static HolidayCalendar fixedOnly() {
        return new HolidayCalendar(Set.of());
    }

    public boolean isWorkingDay(LocalDate d) {
        return d.getDayOfWeek() != DayOfWeek.SUNDAY && !FIXED_HOLIDAYS.contains(MonthDay.from(d)) && !extra.contains(d);
    }

    public LocalDate nextWorkingDay(LocalDate d) {
        LocalDate next = d.plusDays(1);
        while (!isWorkingDay(next)) next = next.plusDays(1);
        return next;
    }

    public LocalDate addWorkingDays(LocalDate start, int days) {
        if (days < 0) throw new IllegalArgumentException("days must not be negative");
        LocalDate d = start;
        for (int i = 0; i < days; i++) d = nextWorkingDay(d);
        return d;
    }
}
