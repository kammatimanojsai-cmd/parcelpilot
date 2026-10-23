package io.parcelpilot.app;

import io.parcelpilot.domain.Shipment;
import io.parcelpilot.domain.TrackingId;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store, capped so a public demo cannot be filled up.
 * The interface keeps a database-backed implementation a drop-in change.
 */
@Repository
public class InMemoryShipmentRepository implements ShipmentRepository {

    static final int CAPACITY = 10_000;

    private final Map<String, Shipment> byId = new ConcurrentHashMap<>();

    @Override
    public boolean insert(Shipment shipment) {
        if (byId.size() >= CAPACITY) {
            throw new IllegalStateException("Demo storage is full");
        }
        return byId.putIfAbsent(shipment.trackingId().value(), shipment) == null;
    }

    @Override
    public Optional<Shipment> find(TrackingId id) {
        return Optional.ofNullable(byId.get(id.value()));
    }

    @Override
    public long count() {
        return byId.size();
    }
}
