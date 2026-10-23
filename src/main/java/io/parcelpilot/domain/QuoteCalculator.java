package io.parcelpilot.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.parcelpilot.domain.Quote.ChargeCode.*;

/**
 * Prices a parcel. Order of operations (all amounts in paise, each step rounded half up):
 * <ol>
 *   <li>freight = rate card(zone, slabs) × service multiplier</li>
 *   <li>fuel surcharge = 12% of freight</li>
 *   <li>COD fee = max(₹40, 2% of declared value), only with cash on delivery</li>
 *   <li>insurance = 0.5% of declared value, only when it is above ₹5,000</li>
 *   <li>GST = 18% of everything above</li>
 * </ol>
 */
public final class QuoteCalculator {

    public static final int FUEL_SURCHARGE_BP = 1_200;
    public static final int GST_BP = 1_800;
    public static final int COD_BP = 200;
    public static final long COD_MINIMUM_PAISE = 4_000;
    /** ₹50,000: the most a courier will collect in cash. */
    public static final long COD_MAX_VALUE_PAISE = 5_000_000;
    public static final int INSURANCE_BP = 50;
    /** ₹5,000. */
    public static final long INSURANCE_THRESHOLD_PAISE = 500_000;

    private final RateCard rates;
    private final EtaCalculator eta;

    public QuoteCalculator(RateCard rates, EtaCalculator eta) {
        this.rates = Objects.requireNonNull(rates);
        this.eta = Objects.requireNonNull(eta);
    }

    public Quote quote(QuoteRequest r, LocalDateTime bookedAt) {
        Zone zone = Zone.between(r.origin(), r.destination());
        ServiceLevel service = r.service();
        if (service == ServiceLevel.SAME_DAY && zone != Zone.LOCAL) {
            throw DomainException.rule("SAME_DAY_UNAVAILABLE", "Same-day delivery is only available within the same city.");
        }
        if (r.cashOnDelivery()) {
            if (r.declaredValuePaise() == 0) {
                throw DomainException.rule("COD_VALUE_REQUIRED", "Cash on delivery needs a declared value to collect.");
            }
            if (r.declaredValuePaise() > COD_MAX_VALUE_PAISE) {
                throw DomainException.rule("COD_LIMIT_EXCEEDED", "Cash on delivery is limited to ₹50,000.");
            }
        }
        Eta estimate = eta.estimate(zone, service, bookedAt);

        Parcel p = r.parcel();
        List<Quote.Charge> charges = new ArrayList<>();
        long freight = (rates.freightPaise(zone, p.slabs()) * service.multiplierPercent() + 50) / 100;
        charges.add(new Quote.Charge(FREIGHT, service.label() + " freight, " + zone.label().toLowerCase() + ", " + kg(p.chargeableGrams()), freight));
        charges.add(new Quote.Charge(FUEL_SURCHARGE, "Fuel surcharge (12%)", Money.percent(freight, FUEL_SURCHARGE_BP)));
        if (r.cashOnDelivery()) {
            long cod = Math.max(COD_MINIMUM_PAISE, Money.percent(r.declaredValuePaise(), COD_BP));
            charges.add(new Quote.Charge(COD_FEE, "Cash on delivery fee", cod));
        }
        if (r.declaredValuePaise() > INSURANCE_THRESHOLD_PAISE) {
            charges.add(new Quote.Charge(INSURANCE, "Transit insurance (0.5% of declared value)", Money.percent(r.declaredValuePaise(), INSURANCE_BP)));
        }
        long subtotal = charges.stream().mapToLong(Quote.Charge::amountPaise).sum();
        long gst = Money.percent(subtotal, GST_BP);
        charges.add(new Quote.Charge(GST, "GST (18%)", gst));

        return new Quote(zone, service, p.weightGrams(), p.volumetricGrams(), p.chargeableGrams(), charges, subtotal + gst, estimate);
    }

    private static String kg(int grams) {
        return (grams % 1000 == 0 ? Integer.toString(grams / 1000) : Double.toString(grams / 1000.0)) + " kg billed";
    }
}
