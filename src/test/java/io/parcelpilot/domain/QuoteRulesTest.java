package io.parcelpilot.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.parcelpilot.support.Fixtures;
import io.parcelpilot.support.JsonCases;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Business-rule scenarios kept in {@code quote-rules.json}, so a tester can add a case without touching Java.
 * A case either expects an error code or a zone (and optionally a total in paise).
 */
@DisplayName("Quote business rules (data-driven from quote-rules.json)")
class QuoteRulesTest {

    private final QuoteCalculator calculator = Fixtures.calculator();

    @ParameterizedTest(name = "{0}")
    @JsonCases("/data/quote-rules.json")
    void appliesRule(JsonNode c) {
        JsonNode in = c.get("request");
        LocalDateTime bookedAt = c.has("bookedAt") ? LocalDateTime.parse(c.get("bookedAt").asText()) : Fixtures.THURSDAY_10AM;
        JsonNode expect = c.get("expect");

        if (expect.has("error")) {
            DomainException e = assertThrows(DomainException.class, () -> calculator.quote(toRequest(in), bookedAt));
            assertThat(e.code()).isEqualTo(expect.get("error").asText());
            assertThat(e.kind().name()).isEqualTo(expect.get("kind").asText());
        } else {
            Quote q = calculator.quote(toRequest(in), bookedAt);
            assertThat(q.zone().name()).isEqualTo(expect.get("zone").asText());
            if (expect.has("totalPaise")) assertThat(q.totalPaise()).isEqualTo(expect.get("totalPaise").asLong());
        }
    }

    private static QuoteRequest toRequest(JsonNode in) {
        Parcel parcel = new Parcel(in.get("weightGrams").asInt(), in.get("lengthCm").asInt(), in.get("widthCm").asInt(), in.get("heightCm").asInt());
        return new QuoteRequest(Pincode.of(in.get("origin").asText()), Pincode.of(in.get("destination").asText()), parcel,
                ServiceLevel.valueOf(in.get("service").asText()), in.path("cod").asBoolean(false), in.path("declaredPaise").asLong(0));
    }
}
