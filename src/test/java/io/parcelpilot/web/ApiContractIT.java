package io.parcelpilot.web;

import com.fasterxml.jackson.databind.JsonNode;
import io.parcelpilot.support.JsonCases;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Iterator;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract, one row per case in {@code api-cases.json}: method, path, optional body, expected status and a map
 * of JSONPath expressions to expected values. QA can extend the API suite by editing JSON only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfig.class)
@DisplayName("API contract (data-driven from api-cases.json)")
class ApiContractIT {

    @Autowired
    MockMvc mvc;

    @ParameterizedTest(name = "{0}")
    @JsonCases("/data/api-cases.json")
    void honoursContract(JsonNode c) throws Exception {
        MockHttpServletRequestBuilder req = request(HttpMethod.valueOf(c.get("method").asText()), c.get("path").asText())
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON);
        if (c.has("rawBody")) {
            req.contentType(MediaType.APPLICATION_JSON).content(c.get("rawBody").asText());
        } else if (c.has("body")) {
            req.contentType(MediaType.APPLICATION_JSON).content(c.get("body").toString());
        }

        ResultActions result = mvc.perform(req).andExpect(status().is(c.get("status").asInt()));
        if (c.get("status").asInt() >= 400) {
            result.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        for (Iterator<Map.Entry<String, JsonNode>> it = c.path("expect").fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> e = it.next();
            result.andExpect(jsonPath(e.getKey()).value(toJava(e.getValue())));
        }
        for (JsonNode path : c.path("absent")) {
            result.andExpect(jsonPath(path.asText()).doesNotExist());
        }
    }

    private static Object toJava(JsonNode v) {
        if (v.isBoolean()) return v.asBoolean();
        if (v.isInt()) return v.asInt();
        if (v.isIntegralNumber()) return v.asLong();
        if (v.isNull()) return null;
        return v.asText();
    }
}
