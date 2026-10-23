package io.parcelpilot.web;

import io.parcelpilot.domain.Money;
import io.parcelpilot.domain.Parcel;
import io.parcelpilot.domain.Pincode;
import io.parcelpilot.domain.Quote;
import io.parcelpilot.domain.QuoteRequest;
import io.parcelpilot.domain.ServiceLevel;
import io.parcelpilot.domain.Shipment;
import io.parcelpilot.domain.ShipmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies. Bean validation checks shape; the domain checks business ranges. */
public final class Dtos {

    private Dtos() {}

    public record QuoteBody(
            @NotBlank String originPincode,
            @NotBlank String destinationPincode,
            @NotNull Integer weightGrams,
            @NotNull Integer lengthCm,
            @NotNull Integer widthCm,
            @NotNull Integer heightCm,
            @NotNull ServiceLevel service,
            boolean cashOnDelivery,
            @NotNull @DecimalMin("0.00") @Digits(integer = 7, fraction = 2) BigDecimal declaredValue) {

        QuoteRequest toDomain() {
            Parcel parcel = new Parcel(weightGrams, lengthCm, widthCm, heightCm);
            return new QuoteRequest(Pincode.of(originPincode), Pincode.of(destinationPincode), parcel, service,
                    cashOnDelivery, declaredValue.movePointRight(2).longValueExact());
        }
    }

    public record BookingBody(@NotNull @Valid QuoteBody parcel, @NotBlank @Size(max = 80) String receiverName) {}

    public record EventBody(@NotNull ShipmentStatus status, @NotBlank @Size(max = 80) String location, @Size(max = 200) String note) {}

    public record ChargeView(String code, String label, long amountPaise, String amount) {}

    public record EtaView(LocalDate pickupDate, LocalDate estimatedDelivery, int transitDays) {}

    public record QuoteView(String zone, String zoneLabel, String service, int actualWeightGrams, int volumetricWeightGrams,
                            int chargeableWeightGrams, List<ChargeView> charges, long totalPaise, String total, EtaView eta) {

        static QuoteView of(Quote q) {
            List<ChargeView> charges = q.charges().stream()
                    .map(c -> new ChargeView(c.code().name(), c.label(), c.amountPaise(), Money.format(c.amountPaise())))
                    .toList();
            return new QuoteView(q.zone().name(), q.zone().label(), q.service().name(), q.actualGrams(), q.volumetricGrams(),
                    q.chargeableGrams(), charges, q.totalPaise(), Money.format(q.totalPaise()),
                    new EtaView(q.eta().pickupDate(), q.eta().deliveryDate(), q.eta().transitDays()));
        }
    }

    public record EventView(String status, String location, String note, Instant at) {}

    public record ShipmentView(String trackingId, String status, boolean terminal, List<String> nextStatuses, int failedAttempts,
                               String origin, String destination, String receiverName, QuoteView quote, List<EventView> events) {

        static ShipmentView of(Shipment s) {
            ShipmentStatus status = s.status();
            return new ShipmentView(s.trackingId().value(), status.name(), status.terminal(),
                    status.next().stream().map(Enum::name).toList(), s.failedAttempts(),
                    s.request().origin().value(), s.request().destination().value(), s.receiverName(), QuoteView.of(s.quote()),
                    s.events().stream().map(e -> new EventView(e.status().name(), e.location(), e.note(), e.at())).toList());
        }
    }
}
