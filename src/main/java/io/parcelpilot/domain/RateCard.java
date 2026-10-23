package io.parcelpilot.domain;

import java.util.EnumMap;
import java.util.Map;

/** Freight per zone: a price for the first 500 g slab and for each additional slab, in paise. */
public final class RateCard {

    public record Rate(long firstSlabPaise, long additionalSlabPaise) {}

    private final Map<Zone, Rate> rates;

    public RateCard(Map<Zone, Rate> rates) {
        for (Zone z : Zone.values()) {
            if (!rates.containsKey(z)) throw new IllegalArgumentException("Rate card is missing zone " + z);
        }
        this.rates = new EnumMap<>(rates);
    }

    public static RateCard standard() {
        Map<Zone, Rate> m = new EnumMap<>(Zone.class);
        m.put(Zone.LOCAL, new Rate(3_500, 2_000));
        m.put(Zone.STATE, new Rate(4_500, 2_500));
        m.put(Zone.REGIONAL, new Rate(5_500, 3_500));
        m.put(Zone.NATIONAL, new Rate(7_000, 4_500));
        m.put(Zone.REMOTE, new Rate(9_000, 6_000));
        return new RateCard(m);
    }

    public Rate rate(Zone zone) {
        return rates.get(zone);
    }

    public long freightPaise(Zone zone, int slabs) {
        if (slabs < 1) throw new IllegalArgumentException("slabs must be at least 1");
        Rate r = rates.get(zone);
        return r.firstSlabPaise() + (slabs - 1L) * r.additionalSlabPaise();
    }
}
