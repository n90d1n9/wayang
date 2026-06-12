package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Shared dependency and custom-store helpers for skill-management store factories.
 */
final class SkillStoreFactorySupport {

    private SkillStoreFactorySupport() {
    }

    static <T> Map<String, T> customStores(Map<String, T> customStores) {
        return customStores == null ? Map.of() : Map.copyOf(customStores);
    }

    static <T> T customStore(Map<String, T> customStores, String name, String label) {
        T store = customStores.get(name);
        if (store == null) {
            throw new IllegalStateException("No custom " + label + " registered for: " + name);
        }
        return store;
    }

    static <T> SkillStoreConfigValidationResult validateCustomStore(
            Map<String, T> customStores,
            String name,
            String label) {
        if (name == null || !customStores.containsKey(name)) {
            return SkillStoreConfigValidationResult.error(
                    "No custom " + label + " registered for: " + name);
        }
        return SkillStoreConfigValidationResult.valid();
    }

    static <T> T requiredDependency(T dependency, String name, Object kind, String owner) {
        return requiredDependency(dependencyRequirement(dependency, name, kind, owner));
    }

    static <T> T requiredDependency(T dependency, String message, String name) {
        return requiredDependency(dependencyRequirement(dependency, name, message));
    }

    static <T> T requiredDependency(DependencyRequirement<T> requirement) {
        if (requirement.value() == null) {
            throw new IllegalStateException(requirement.missingMessage());
        }
        return Objects.requireNonNull(requirement.value(), requirement.name());
    }

    static SkillStoreConfigValidationResult validateRequiredDependency(
            Object dependency,
            String name,
            Object kind,
            String owner) {
        return validateRequiredDependency(dependencyRequirement(dependency, name, kind, owner));
    }

    static SkillStoreConfigValidationResult validateRequiredDependency(Object dependency, String message) {
        return validateRequiredDependency(dependencyRequirement(dependency, "dependency", message));
    }

    static SkillStoreConfigValidationResult validateRequiredDependency(DependencyRequirement<?> requirement) {
        return requirement.value() == null
                ? SkillStoreConfigValidationResult.error(requirement.missingMessage())
                : SkillStoreConfigValidationResult.valid();
    }

    static <T> DependencyRequirement<T> dependencyRequirement(
            T dependency,
            String name,
            Object kind,
            String owner) {
        return dependencyRequirement(
                dependency,
                name,
                owner + " kind " + kind + " requires dependency: " + name);
    }

    static <T> DependencyRequirement<T> dependencyRequirement(
            T dependency,
            String name,
            String missingMessage) {
        return new DependencyRequirement<>(
                dependency,
                Objects.requireNonNull(name, "name"),
                Objects.requireNonNull(missingMessage, "missingMessage"));
    }

    static <T> SkillStoreConfigValidationResult validatePrimaryFallback(
            T primary,
            T fallback,
            Function<T, SkillStoreConfigValidationResult> validator) {
        return SkillStoreConfigValidationResult.combine(
                primary == null ? SkillStoreConfigValidationResult.valid() : validator.apply(primary),
                fallback == null ? SkillStoreConfigValidationResult.valid() : validator.apply(fallback));
    }

    static <T, R> PrimaryFallback<R> createPrimaryFallback(
            T primary,
            T fallback,
            Function<T, R> creator) {
        return new PrimaryFallback<>(
                creator.apply(primary),
                creator.apply(fallback));
    }

    static <T, R, C> C createPrimaryFallback(
            T primary,
            T fallback,
            Function<T, R> creator,
            BiFunction<R, R, C> composer) {
        PrimaryFallback<R> stores = createPrimaryFallback(primary, fallback, creator);
        return composer.apply(stores.primary(), stores.fallback());
    }

    record PrimaryFallback<T>(T primary, T fallback) {
    }

    record DependencyRequirement<T>(T value, String name, String missingMessage) {
    }
}
