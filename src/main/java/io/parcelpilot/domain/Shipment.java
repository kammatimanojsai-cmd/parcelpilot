package io.parcelpilot.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A booked parcel and its tracking timeline. All changes go through {@link #advance}. */
public final class Shipment {

    public static final int MAX_DELIVERY_ATTEMPTS = 3;

    public record Event(ShipmentStatus status, String location, String note, Instant at) {}

    private final TrackingId trackingId;
    private final QuoteRequest request;
    private final Quote quote;
    private final String receiverName;
    private final List<Event> events = new ArrayList<>();
    private ShipmentStatus status;
    private int failedAttempts;

    private Shipment(TrackingId trackingId, QuoteRequest request, Quote quote, String receiverName) {
        this.trackingId = Objects.requireNonNull(trackingId);
        this.request = Objects.requireNonNull(request);
        this.quote = Objects.requireNonNull(quote);
        this.receiverName = Objects.requireNonNull(receiverName);
    }

    public static Shipment book(TrackingId id, QuoteRequest request, Quote quote, String receiverName, Instant at) {
        Shipment s = new Shipment(id, request, quote, receiverName);
        s.status = ShipmentStatus.BOOKED;
        s.events.add(new Event(ShipmentStatus.BOOKED, request.origin().value(), "Booking confirmed", at));
        return s;
    }

    public synchronized Event advance(ShipmentStatus target, String location, String note, Instant at) {
        Objects.requireNonNull(target, "target");
        if (!status.canMoveTo(target)) {
            throw new DomainException(DomainException.Kind.CONFLICT, "ILLEGAL_TRANSITION",
                    "A shipment that is " + status + " cannot move to " + target + ".");
        }
        if (status == ShipmentStatus.DELIVERY_FAILED && target == ShipmentStatus.OUT_FOR_DELIVERY
                && failedAttempts >= MAX_DELIVERY_ATTEMPTS) {
            throw new DomainException(DomainException.Kind.CONFLICT, "ATTEMPTS_EXHAUSTED",
                    "Delivery failed " + MAX_DELIVERY_ATTEMPTS + " times; the parcel must be returned to origin.");
        }
        if (target == ShipmentStatus.DELIVERY_FAILED) failedAttempts++;
        status = target;
        Event e = new Event(target, location, note, at);
        events.add(e);
        return e;
    }

    public TrackingId trackingId() { return trackingId; }
    public QuoteRequest request() { return request; }
    public Quote quote() { return quote; }
    public String receiverName() { return receiverName; }
    public synchronized ShipmentStatus status() { return status; }
    public synchronized int failedAttempts() { return failedAttempts; }
    public synchronized List<Event> events() { return List.copyOf(events); }
}
