package io.parcelpilot.domain;

/** Delivery zone between two PIN codes. Each zone has its own rate card row and transit time. */
public enum Zone {
    LOCAL("Same city"),
    STATE("Same state or circle"),
    REGIONAL("Same postal region"),
    NATIONAL("Across India"),
    REMOTE("Remote area");

    private final String label;

    Zone(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Same sorting district wins; otherwise a remote end makes it REMOTE; then the shared prefix decides. */
    public static Zone between(Pincode from, Pincode to) {
        if (from.sharesPrefix(to, 3)) return LOCAL;
        if (from.remote() || to.remote()) return REMOTE;
        if (from.sharesPrefix(to, 2)) return STATE;
        if (from.sharesPrefix(to, 1)) return REGIONAL;
        return NATIONAL;
    }
}
