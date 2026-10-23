package io.parcelpilot.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end journeys over HTTP: book a parcel, then drive it through its lifecycle. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfig.class)
@DisplayName("Shipment journeys over HTTP")
class ShipmentJourneyIT {

    private static final String PARCEL = """
            {"originPincode":"560001","destinationPincode":"110001","weightGrams":1200,"lengthCm":30,"widthCm":20,
             "heightCm":10,"service":"EXPRESS","cashOnDelivery":true,"declaredValue":2499}""";

    @Autowired
    MockMvc mvc;

    private String book() throws Exception {
        String json = mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parcel\":" + PARCEL + ",\"receiverName\":\"  Asha Rao \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.receiverName").value("Asha Rao"))
                .andExpect(jsonPath("$.nextStatuses", hasSize(2)))
                .andExpect(jsonPath("$.quote.zone").value("NATIONAL"))
                .andExpect(jsonPath("$.events[0].at").value("2026-10-22T04:30:00Z"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(json, "$.trackingId");
        assertThat(id).matches("PP\\d{10}");
        return id;
    }

    private void move(String id, String status, int expectedHttp) throws Exception {
        mvc.perform(post("/api/shipments/{id}/events", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\",\"location\":\"Delhi hub\",\"note\":\" scanned \"}"))
                .andExpect(status().is(expectedHttp));
    }

    @Test
    void bookingReturnsLocationAndCanBeTracked() throws Exception {
        String id = book();

        mvc.perform(get("/api/shipments/{id}", id.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingId").value(id))
                .andExpect(jsonPath("$.quote.charges[2].code").value("COD_FEE"))
                .andExpect(jsonPath("$.events", hasSize(1)));

        var response = mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parcel\":" + PARCEL + ",\"receiverName\":\"Ravi\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse();
        String second = JsonPath.read(response.getContentAsString(), "$.trackingId");
        assertThat(second).isNotEqualTo(id);
        assertThat(response.getHeader("Location")).endsWith("/api/shipments/" + second);
    }

    /** Each row is a journey: the HTTP status expected for every step, in order. */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            delivered first time            | PICKED_UP:200 > IN_TRANSIT:200 > OUT_FOR_DELIVERY:200 > DELIVERED:200            | DELIVERED
            cancelled, then nothing else    | CANCELLED:200 > PICKED_UP:409                                                    | CANCELLED
            out of order step is refused    | IN_TRANSIT:409 > PICKED_UP:200                                                   | PICKED_UP
            three failures, then returned   | PICKED_UP:200 > IN_TRANSIT:200 > OUT_FOR_DELIVERY:200 > DELIVERY_FAILED:200 > OUT_FOR_DELIVERY:200 > DELIVERY_FAILED:200 > OUT_FOR_DELIVERY:200 > DELIVERY_FAILED:200 > OUT_FOR_DELIVERY:409 > RETURNED_TO_ORIGIN:200 | RETURNED_TO_ORIGIN
            """)
    void journey(String name, String steps, String finalStatus) throws Exception {
        String id = book();
        int applied = 0;
        for (String step : steps.split(">")) {
            String[] parts = step.trim().split(":");
            int http = Integer.parseInt(parts[1]);
            move(id, parts[0], http);
            if (http == 200) applied++;
        }
        mvc.perform(get("/api/shipments/{id}", id))
                .andExpect(jsonPath("$.status").value(finalStatus))
                .andExpect(jsonPath("$.events", hasSize(1 + applied)))
                .andExpect(jsonPath("$.events[1].note").value("scanned"));
    }

    @Test
    void conflictExplainsWhy() throws Exception {
        String id = book();
        mvc.perform(post("/api/shipments/{id}/events", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\",\"location\":\"Delhi\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"))
                .andExpect(jsonPath("$.detail").value("A shipment that is BOOKED cannot move to DELIVERED."));
    }

    @Test
    void servesTheDemoPage() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/index.html")).andExpect(status().isOk());
    }
}
