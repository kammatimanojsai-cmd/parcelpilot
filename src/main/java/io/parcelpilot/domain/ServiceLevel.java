package io.parcelpilot.domain;

public enum ServiceLevel {
    STANDARD("Standard", 100),
    EXPRESS("Express", 160),
    SAME_DAY("Same day", 250);

    private final String label;
    private final int multiplierPercent;

    ServiceLevel(String label, int multiplierPercent) {
        this.label = label;
        this.multiplierPercent = multiplierPercent;
    }

    public String label() {
        return label;
    }

    /** Freight multiplier in percent of the standard rate. */
    public int multiplierPercent() {
        return multiplierPercent;
    }
}
