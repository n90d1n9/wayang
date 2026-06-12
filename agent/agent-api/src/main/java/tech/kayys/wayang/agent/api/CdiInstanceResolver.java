package tech.kayys.wayang.agent.api;

import jakarta.enterprise.inject.Instance;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Resolves optional CDI instances while allowing tests to provide explicit overrides.
 */
final class CdiInstanceResolver {

    <T> Optional<T> first(T override, Instance<T> instances) {
        if (instances == null) {
            return Optional.ofNullable(override);
        }
        return first(override, instances.stream());
    }

    <T> Optional<T> first(T override, Stream<T> instances) {
        if (override != null) {
            return Optional.of(override);
        }
        if (instances == null) {
            return Optional.empty();
        }
        return instances
                .filter(Objects::nonNull)
                .findFirst();
    }
}
