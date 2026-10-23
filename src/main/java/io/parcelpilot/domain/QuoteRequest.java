package io.parcelpilot.domain;

import java.util.Objects;

/** Everything needed to price a parcel. Declared value is the parcel's worth (and the COD amount when COD is on). */
public record QuoteRequest(Pincode origin, Pincode destination, Parcel parcel, ServiceLevel service,
                           boolean cashOnDelivery, long declaredValuePaise) {

    /** ₹2,00,000. */
    public static final long MAX_DECLARED_VALUE_PAISE = 20_000_000L;

    public QuoteRequest {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(parcel, "parcel");
        Objects.requireNonNull(service, "service");
        if (declaredValuePaise < 0 || declaredValuePaise > MAX_DECLARED_VALUE_PAISE) {
            throw DomainException.invalid("DECLARED_VALUE_OUT_OF_RANGE", "Declared value must be between ₹0 and ₹2,00,000.");
        }
    }
}
