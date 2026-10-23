package io.parcelpilot.support;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Feeds a {@code @ParameterizedTest} from a JSON array on the classpath. Each element becomes one invocation,
 * passed as a Jackson {@code JsonNode} and named after its {@code "name"} field.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(JsonCasesProvider.class)
public @interface JsonCases {

    /** Classpath resource, e.g. {@code /data/api-cases.json}. */
    String value();
}
