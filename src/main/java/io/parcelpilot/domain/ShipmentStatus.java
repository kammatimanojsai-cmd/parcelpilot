package io.parcelpilot.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static java.util.EnumSet.noneOf;
import static java.util.EnumSet.of;

/** Shipment lifecycle. The allowed moves are the single source of truth for the API and the tests' data tables. */
public enum ShipmentStatus {
    BOOKED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERY_FAILED, DELIVERED, CANCELLED, RETURNED_TO_ORIGIN;

    private static final Map<ShipmentStatus, EnumSet<ShipmentStatus>> NEXT = new EnumMap<>(ShipmentStatus.class);

    static {
        NEXT.put(BOOKED, of(PICKED_UP, CANCELLED));
        NEXT.put(PICKED_UP, of(IN_TRANSIT));
        NEXT.put(IN_TRANSIT, of(OUT_FOR_DELIVERY));
        NEXT.put(OUT_FOR_DELIVERY, of(DELIVERED, DELIVERY_FAILED));
        NEXT.put(DELIVERY_FAILED, of(OUT_FOR_DELIVERY, RETURNED_TO_ORIGIN));
        NEXT.put(DELIVERED, noneOf(ShipmentStatus.class));
        NEXT.put(CANCELLED, noneOf(ShipmentStatus.class));
        NEXT.put(RETURNED_TO_ORIGIN, noneOf(ShipmentStatus.class));
    }

    public Set<ShipmentStatus> next() {
        return Collections.unmodifiableSet(EnumSet.copyOf(NEXT.get(this)));
    }

    public boolean canMoveTo(ShipmentStatus target) {
        return NEXT.get(this).contains(target);
    }

    public boolean terminal() {
        return NEXT.get(this).isEmpty();
    }
}
