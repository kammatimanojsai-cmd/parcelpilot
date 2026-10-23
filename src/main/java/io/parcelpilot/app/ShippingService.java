package io.parcelpilot.app;

import io.parcelpilot.domain.DomainException;
import io.parcelpilot.domain.Quote;
import io.parcelpilot.domain.QuoteCalculator;
import io.parcelpilot.domain.QuoteRequest;
import io.parcelpilot.domain.Shipment;
import io.parcelpilot.domain.ShipmentStatus;
import io.parcelpilot.domain.TrackingId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.random.RandomGenerator;

@Service
public class ShippingService {

    private final QuoteCalculator calculator;
    private final ShipmentRepository repository;
    private final Clock clock;
    private final RandomGenerator random;

    public ShippingService(QuoteCalculator calculator, ShipmentRepository repository, Clock clock, RandomGenerator random) {
        this.calculator = calculator;
        this.repository = repository;
        this.clock = clock;
        this.random = random;
    }

    public Quote quote(QuoteRequest request) {
        return calculator.quote(request, LocalDateTime.now(clock));
    }

    public Shipment book(QuoteRequest request, String receiverName) {
        Quote quote = quote(request);
        for (int attempt = 0; attempt < 5; attempt++) {
            Shipment s = Shipment.book(TrackingId.generate(random), request, quote, receiverName, clock.instant());
            if (repository.insert(s)) return s;
        }
        throw new IllegalStateException("Could not allocate a unique tracking number");
    }

    public Shipment track(String rawTrackingId) {
        TrackingId id = TrackingId.parse(rawTrackingId);
        return repository.find(id).orElseThrow(() -> new DomainException(DomainException.Kind.NOT_FOUND,
                "SHIPMENT_NOT_FOUND", "No shipment has tracking number " + id + "."));
    }

    public Shipment advance(String rawTrackingId, ShipmentStatus status, String location, String note) {
        Shipment s = track(rawTrackingId);
        s.advance(status, location, note, clock.instant());
        return s;
    }
}
