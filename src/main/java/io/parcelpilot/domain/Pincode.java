package io.parcelpilot.domain;

import java.util.List;
import java.util.regex.Pattern;

/**
 * An Indian postal PIN code: six digits, never starting with 0.
 * The first digit is the postal region, the first two the sub-region and the first three the sorting district.
 */
public record Pincode(String value) {

    private static final Pattern FORMAT = Pattern.compile("[1-9][0-9]{5}");

    /** Prefixes served as remote areas: Jammu and Kashmir and Ladakh (18, 19), the North East (79), Andaman and Nicobar (744). */
    static final List<String> REMOTE_PREFIXES = List.of("18", "19", "79", "744");

    public Pincode {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw DomainException.invalid("INVALID_PINCODE", "A PIN code is 6 digits and cannot start with 0.");
        }
    }

    public static Pincode of(String raw) {
        return new Pincode(raw == null ? null : raw.strip());
    }

    public boolean remote() {
        return REMOTE_PREFIXES.stream().anyMatch(value::startsWith);
    }

    boolean sharesPrefix(Pincode other, int digits) {
        return value.regionMatches(0, other.value, 0, digits);
    }

    @Override
    public String toString() {
        return value;
    }
}
