package io.parcelpilot.web;

import io.parcelpilot.app.ShippingService;
import io.parcelpilot.domain.Shipment;
import io.parcelpilot.web.Dtos.BookingBody;
import io.parcelpilot.web.Dtos.EventBody;
import io.parcelpilot.web.Dtos.QuoteBody;
import io.parcelpilot.web.Dtos.QuoteView;
import io.parcelpilot.web.Dtos.ShipmentView;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class ShippingController {

    private final ShippingService service;

    public ShippingController(ShippingService service) {
        this.service = service;
    }

    @PostMapping("/quotes")
    public QuoteView quote(@Valid @RequestBody QuoteBody body) {
        return QuoteView.of(service.quote(body.toDomain()));
    }

    @PostMapping("/shipments")
    public ResponseEntity<ShipmentView> book(@Valid @RequestBody BookingBody body) {
        Shipment s = service.book(body.parcel().toDomain(), body.receiverName().strip());
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(s.trackingId().value()).toUri();
        return ResponseEntity.created(location).body(ShipmentView.of(s));
    }

    @GetMapping("/shipments/{trackingId}")
    public ShipmentView track(@PathVariable String trackingId) {
        return ShipmentView.of(service.track(trackingId));
    }

    @PostMapping("/shipments/{trackingId}/events")
    public ShipmentView addEvent(@PathVariable String trackingId, @Valid @RequestBody EventBody body) {
        String note = body.note() == null ? null : body.note().strip();
        return ShipmentView.of(service.advance(trackingId, body.status(), body.location().strip(), note));
    }
}
