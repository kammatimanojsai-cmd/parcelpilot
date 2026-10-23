package io.parcelpilot.app;

import io.parcelpilot.domain.DomainException;
import io.parcelpilot.domain.Parcel;
import io.parcelpilot.domain.Pincode;
import io.parcelpilot.domain.QuoteRequest;
import io.parcelpilot.domain.ServiceLevel;
import io.parcelpilot.domain.Shipment;
import io.parcelpilot.domain.ShipmentStatus;
import io.parcelpilot.support.Fixtures;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShippingServiceTest {

    private final QuoteRequest request = new QuoteRequest(Pincode.of("560001"), Pincode.of("560034"),
            new Parcel(800, 20, 15, 10), ServiceLevel.STANDARD, false, 0);

    private ShippingService service(RandomGenerator random, ShipmentRepository repo) {
        return new ShippingService(Fixtures.calculator(), repo, Fixtures.thursdayMorning(), random);
    }

    @Test
    void booksTracksAndAdvances() {
        var repo = new InMemoryShipmentRepository();
        ShippingService svc = service(new SplittableRandom(1), repo);

        Shipment booked = svc.book(request, "Asha");
        Shipment found = svc.track(booked.trackingId().value().toLowerCase());
        svc.advance(booked.trackingId().value(), ShipmentStatus.PICKED_UP, "Koramangala", "Picked up by Ravi");

        assertThat(found).isSameAs(booked);
        assertThat(booked.status()).isEqualTo(ShipmentStatus.PICKED_UP);
        assertThat(booked.events().get(0).at()).isEqualTo(Fixtures.thursdayMorning().instant());
        assertThat(repo.count()).isEqualTo(1);
    }

    @Test
    void unknownTrackingNumberIsNotFound() {
        assertThatThrownBy(() -> service(new SplittableRandom(1), new InMemoryShipmentRepository()).track("PP1234567897"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).kind()).isEqualTo(DomainException.Kind.NOT_FOUND));
    }

    @Test
    void retriesOnCollisionThenGivesUp() {
        RandomGenerator alwaysSame = () -> 42L;
        ShippingService svc = service(alwaysSame, new InMemoryShipmentRepository());

        svc.book(request, "First");

        assertThatThrownBy(() -> svc.book(request, "Second"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unique tracking number");
    }

    @Test
    void demoStorageIsCapped() {
        var repo = new InMemoryShipmentRepository();
        ShippingService svc = service(new SplittableRandom(3), repo);
        for (int i = 0; i < InMemoryShipmentRepository.CAPACITY; i++) svc.book(request, "R" + i);

        assertThatThrownBy(() -> svc.book(request, "One too many")).isInstanceOf(IllegalStateException.class);
        assertThat(repo.count()).isEqualTo(InMemoryShipmentRepository.CAPACITY);
    }
}
