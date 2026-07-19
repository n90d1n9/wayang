package tech.kayys.wayang.agent.skills.management;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Registry of leaf store providers for role-specific store factories.
 */
final class SkillStoreProviderRegistry<K, C, S> {

    private final Map<K, Provider<C, S>> providers;

    private SkillStoreProviderRegistry(Map<K, Provider<C, S>> providers) {
        this.providers = Collections.unmodifiableMap(new LinkedHashMap<>(providers));
    }

    static <K, C, S> Builder<K, C, S> builder() {
        return new Builder<>();
    }

    boolean supports(K kind) {
        return providers.containsKey(kind);
    }

    Set<K> kinds() {
        return providers.keySet();
    }

    S create(K kind, C config) {
        return provider(kind).create(config);
    }

    SkillStoreConfigValidationResult validate(K kind, C config) {
        return provider(kind).validate(config);
    }

    private Provider<C, S> provider(K kind) {
        Provider<C, S> provider = providers.get(kind);
        if (provider == null) {
            throw new IllegalArgumentException("No skill store provider registered for kind: " + kind);
        }
        return provider;
    }

    static final class Builder<K, C, S> {

        private final Map<K, Provider<C, S>> providers = new LinkedHashMap<>();

        Builder<K, C, S> register(
                K kind,
                Function<C, S> creator,
                Function<C, SkillStoreConfigValidationResult> validator) {
            K resolvedKind = Objects.requireNonNull(kind, "kind");
            if (providers.containsKey(resolvedKind)) {
                throw new IllegalArgumentException(
                        "Duplicate skill store provider registered for kind: " + resolvedKind);
            }
            providers.put(resolvedKind, new Provider<>(creator, validator));
            return this;
        }

        SkillStoreProviderRegistry<K, C, S> build() {
            return new SkillStoreProviderRegistry<>(providers);
        }
    }

    private record Provider<C, S>(
            Function<C, S> creator,
            Function<C, SkillStoreConfigValidationResult> validator) {

        private Provider {
            creator = Objects.requireNonNull(creator, "creator");
            validator = Objects.requireNonNull(validator, "validator");
        }

        S create(C config) {
            return creator.apply(config);
        }

        SkillStoreConfigValidationResult validate(C config) {
            return validator.apply(config);
        }
    }
}
