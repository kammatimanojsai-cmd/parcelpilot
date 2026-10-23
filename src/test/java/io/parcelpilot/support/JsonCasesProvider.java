package io.parcelpilot.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class JsonCasesProvider implements ArgumentsProvider, AnnotationConsumer<JsonCases> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String resource;

    @Override
    public void accept(JsonCases annotation) {
        this.resource = annotation.value();
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        JsonNode root;
        try (InputStream in = JsonCasesProvider.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalArgumentException("Test data not found on the classpath: " + resource);
            root = MAPPER.readTree(in);
        }
        if (!root.isArray() || root.isEmpty()) {
            throw new IllegalArgumentException(resource + " must be a non-empty JSON array");
        }
        List<Arguments> cases = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (JsonNode node : root) {
            String name = node.path("name").asText("");
            if (name.isBlank() || !names.add(name)) {
                throw new IllegalArgumentException(resource + ": every case needs a unique \"name\" (problem at: " + name + ")");
            }
            cases.add(Arguments.of(Named.of(name, node)));
        }
        return cases.stream();
    }
}
