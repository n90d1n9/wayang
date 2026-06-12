package tech.kayys.wayang.agent.hermes;

import java.util.Optional;

/**
 * Null-safe Optional helpers shared by Hermes assembly and resolver code.
 */
final class HermesOptionals {

    private HermesOptionals() {
    }

    static <T> Optional<T> orEmpty(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }
}
