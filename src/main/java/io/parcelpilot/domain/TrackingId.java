package io.parcelpilot.domain;

import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

/**
 * Tracking numbers look like {@code PP4821937065}: "PP", nine random digits and a Luhn check digit,
 * so a mistyped digit is rejected before any lookup.
 */
public record TrackingId(String value) {

    private static final Pattern FORMAT = Pattern.compile("PP[0-9]{10}");

    public TrackingId {
        if (!isValid(value)) {
            throw DomainException.invalid("INVALID_TRACKING_ID", "A tracking number is PP followed by 10 digits, with a valid check digit.");
        }
    }

    public static TrackingId parse(String raw) {
        return new TrackingId(raw == null ? null : raw.strip().toUpperCase());
    }

    public static TrackingId generate(RandomGenerator random) {
        String payload = String.format("%09d", random.nextInt(1_000_000_000));
        return new TrackingId("PP" + payload + checkDigit(payload));
    }

    public static boolean isValid(String raw) {
        return raw != null && FORMAT.matcher(raw).matches() && luhnSum(raw.substring(2), false) % 10 == 0;
    }

    /** The digit that makes {@code payload + digit} pass the Luhn check. */
    static int checkDigit(String payload) {
        return (10 - luhnSum(payload, true) % 10) % 10;
    }

    /** Luhn sum; {@code doubleRightmost} is true when the check digit is not yet appended. */
    private static int luhnSum(String digits, boolean doubleRightmost) {
        int sum = 0;
        for (int i = digits.length() - 1, pos = 0; i >= 0; i--, pos++) {
            int d = digits.charAt(i) - '0';
            if ((pos % 2 == 0) == doubleRightmost) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
        }
        return sum;
    }

    @Override
    public String toString() {
        return value;
    }
}
