package tech.kayys.wayang.agent.skills.management;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Shared merge rules for primary/fallback skill-management stores.
 */
final class HybridSkillStoreSupport {

    private HybridSkillStoreSupport() {
    }

    static <T> Optional<T> primaryOrFallback(
            Supplier<Optional<T>> primary,
            Supplier<Optional<T>> fallback) {
        return primaryOrFallback(primary, fallback, null);
    }

    static <T> Optional<T> primaryOrFallback(
            Supplier<Optional<T>> primary,
            Supplier<Optional<T>> fallback,
            Consumer<T> repairPrimary) {
        Optional<T> primaryValue;
        try {
            primaryValue = primary.get();
        } catch (RuntimeException ignored) {
            return fallback.get();
        }
        if (primaryValue.isPresent()) {
            return primaryValue;
        }
        Optional<T> fallbackValue = fallback.get();
        fallbackValue.ifPresent(value -> repair(repairPrimary, value));
        return fallbackValue;
    }

    static boolean removeFromBoth(BooleanSupplier primary, BooleanSupplier fallback) {
        boolean removedPrimary = primary.getAsBoolean();
        boolean removedFallback = fallback.getAsBoolean();
        return removedPrimary || removedFallback;
    }

    static <T, K> List<T> mergeFallbackThenPrimary(
            Supplier<? extends List<? extends T>> fallback,
            Supplier<? extends List<? extends T>> primary,
            Function<? super T, ? extends K> key) {
        List<? extends T> fallbackItems = fallback.get();
        List<? extends T> primaryItems;
        try {
            primaryItems = primary.get();
        } catch (RuntimeException ignored) {
            return List.copyOf(fallbackItems);
        }
        return mergeFallbackThenPrimary(fallbackItems, primaryItems, key);
    }

    static <T, K> List<T> mergeFallbackThenPrimary(
            List<? extends T> fallback,
            List<? extends T> primary,
            Function<? super T, ? extends K> key) {
        Map<K, T> merged = new LinkedHashMap<>();
        fallback.forEach(item -> merged.put(key.apply(item), item));
        primary.forEach(item -> merged.put(key.apply(item), item));
        return List.copyOf(merged.values());
    }

    static <K, V> Map<K, V> mergeFallbackThenPrimary(
            Supplier<? extends Map<? extends K, ? extends V>> fallback,
            Supplier<? extends Map<? extends K, ? extends V>> primary) {
        Map<? extends K, ? extends V> fallbackItems = fallback.get();
        Map<? extends K, ? extends V> primaryItems;
        try {
            primaryItems = primary.get();
        } catch (RuntimeException ignored) {
            return Map.copyOf(fallbackItems);
        }
        return mergeFallbackThenPrimary(fallbackItems, primaryItems);
    }

    static <K, V> Map<K, V> mergeFallbackThenPrimary(
            Map<? extends K, ? extends V> fallback,
            Map<? extends K, ? extends V> primary) {
        Map<K, V> merged = new LinkedHashMap<>();
        fallback.forEach(merged::put);
        primary.forEach(merged::put);
        return Map.copyOf(merged);
    }

    private static <T> void repair(Consumer<T> repairPrimary, T value) {
        if (repairPrimary == null) {
            return;
        }
        try {
            repairPrimary.accept(value);
        } catch (RuntimeException ignored) {
        }
    }
}
