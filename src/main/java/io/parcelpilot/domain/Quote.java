package io.parcelpilot.domain;

import java.util.List;

/** A priced quote: an itemised list of charges that always sums to {@link #totalPaise()}. */
public record Quote(Zone zone, ServiceLevel service, int actualGrams, int volumetricGrams, int chargeableGrams,
                    List<Charge> charges, long totalPaise, Eta eta) {

    public enum ChargeCode { FREIGHT, FUEL_SURCHARGE, COD_FEE, INSURANCE, GST }

    public record Charge(ChargeCode code, String label, long amountPaise) {}

    public Quote {
        charges = List.copyOf(charges);
        long sum = charges.stream().mapToLong(Charge::amountPaise).sum();
        if (sum != totalPaise) throw new IllegalStateException("charges " + sum + " do not add up to total " + totalPaise);
    }

    public long amount(ChargeCode code) {
        return charges.stream().filter(c -> c.code() == code).mapToLong(Charge::amountPaise).sum();
    }
}
