package io.parcelpilot.app;

import io.parcelpilot.domain.Shipment;
import io.parcelpilot.domain.TrackingId;

import java.util.Optional;

public interface ShipmentRepository {

    /** Stores the shipment unless its tracking id is taken; returns false on a collision. */
    boolean insert(Shipment shipment);

    Optional<Shipment> find(TrackingId id);

    long count();
}
