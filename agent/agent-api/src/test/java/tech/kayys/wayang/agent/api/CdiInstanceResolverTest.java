package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CdiInstanceResolverTest {

    private final CdiInstanceResolver resolver = new CdiInstanceResolver();

    @Test
    void returnsOverrideBeforeInjectedInstances() {
        Optional<String> resolved = resolver.first("override", Stream.of("injected"));

        assertThat(resolved).contains("override");
    }

    @Test
    void returnsFirstInjectedInstanceWhenOverrideIsMissing() {
        Optional<String> resolved = resolver.first(null, Stream.of("first", "second"));

        assertThat(resolved).contains("first");
    }

    @Test
    void ignoresNullInjectedValues() {
        Optional<String> resolved = resolver.first(null, Stream.of(null, "configured"));

        assertThat(resolved).contains("configured");
    }

    @Test
    void returnsEmptyWhenNothingIsConfigured() {
        assertThat(resolver.first(null, (Stream<String>) null)).isEmpty();
        assertThat(resolver.first(null, Stream.<String>empty())).isEmpty();
    }
}
