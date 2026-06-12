package tech.kayys.wayang.agent.skills.management;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * Shared formatting helpers for read-only skill store diagnostics.
 */
final class SkillStoreInspectionSupport {

    private SkillStoreInspectionSupport() {
    }

    static <T> T require(T store, String name) {
        return Objects.requireNonNull(store, name);
    }

    static String storeType(Object store) {
        String simpleName = store.getClass().getSimpleName();
        if (simpleName == null || simpleName.isBlank()) {
            return store.getClass().getName();
        }
        return simpleName;
    }

    static int count(int count) {
        return SkillManagementValueSupport.nonNegative(count);
    }

    static int countAtLeast(int count, int minimum) {
        return SkillManagementValueSupport.atLeast(count, minimum);
    }

    static String text(String value) {
        return SkillManagementValueSupport.text(value);
    }

    static List<String> ids(List<String> values) {
        return SkillManagementValueSupport.compactStrings(values);
    }

    static <T> List<T> children(List<T> values) {
        return SkillManagementValueSupport.nonNullList(values);
    }

    static Map<String, Integer> counts(Map<String, Integer> values) {
        return SkillManagementValueSupport.nonNegativeCounts(values);
    }

    static Map<SkillLifecycleStatus, Integer> lifecycleStatusCounts(
            Map<SkillLifecycleStatus, Integer> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        EnumMap<SkillLifecycleStatus, Integer> copy = new EnumMap<>(SkillLifecycleStatus.class);
        values.forEach((status, count) -> {
            if (status != null && count != null && count > 0) {
                copy.put(status, count);
            }
        });
        return Map.copyOf(copy);
    }

    static SkillStoreCapabilities definitionCapabilities(SkillDefinitionStore store) {
        SkillStoreCapabilities capabilities = SkillStoreCapabilities.definitionStore();
        if (store instanceof HybridSkillDefinitionStore) {
            capabilities = capabilities.with(SkillStoreCapability.PRIMARY_FALLBACK);
        }
        if (store instanceof MirroredSkillDefinitionStore) {
            capabilities = capabilities
                    .with(SkillStoreCapability.PRIMARY_FALLBACK)
                    .with(SkillStoreCapability.MIRROR_WRITE);
        }
        return capabilities;
    }

    static SkillStoreCapabilities lifecycleCapabilities(SkillLifecycleStateStore store) {
        SkillStoreCapabilities capabilities = SkillStoreCapabilities.lifecycleStateStore();
        if (store instanceof HybridSkillLifecycleStateStore) {
            capabilities = capabilities.with(SkillStoreCapability.PRIMARY_FALLBACK);
        }
        if (store instanceof MirroredSkillLifecycleStateStore) {
            capabilities = capabilities
                    .with(SkillStoreCapability.PRIMARY_FALLBACK)
                    .with(SkillStoreCapability.MIRROR_WRITE);
        }
        return capabilities;
    }

    static SkillStoreCapabilities eventCapabilities(Object store) {
        return SkillStoreCapabilities.eventStore(store);
    }

    static SkillStoreCapabilities artifactCapabilities(SkillArtifactStore store) {
        SkillStoreCapabilities capabilities = SkillStoreCapabilities.of(
                SkillStoreCapability.READ,
                SkillStoreCapability.WRITE,
                SkillStoreCapability.DELETE,
                SkillStoreCapability.LIST);
        if (store instanceof HybridSkillArtifactStore) {
            capabilities = capabilities.with(SkillStoreCapability.PRIMARY_FALLBACK);
        }
        if (store instanceof MirroredSkillArtifactStore) {
            capabilities = capabilities
                    .with(SkillStoreCapability.PRIMARY_FALLBACK)
                    .with(SkillStoreCapability.MIRROR_WRITE);
        }
        return capabilities;
    }

    static List<String> sortedNonBlankIds(Stream<String> skillIds) {
        return skillIds
                .filter(skillId -> skillId != null && !skillId.isBlank())
                .sorted()
                .toList();
    }

    static <T, R> List<R> primaryFallbackChildren(
            T primary,
            T fallback,
            BiFunction<String, T, R> inspect) {
        return List.of(
                inspect.apply("primary", primary),
                inspect.apply("fallback", fallback));
    }

    static String indexedChildName(int index, int size, String prefix) {
        if (size == 2) {
            return index == 0 ? "primary" : "fallback";
        }
        return prefix + "-" + (index + 1);
    }

    static String errorMessage(RuntimeException error) {
        List<Throwable> chain = causalChain(error);
        Throwable root = chain.isEmpty() ? error : chain.get(chain.size() - 1);
        String message = root.getMessage();
        return message == null || message.isBlank()
                ? root.getClass().getSimpleName()
                : root.getClass().getSimpleName() + ": " + message;
    }

    private static List<Throwable> causalChain(Throwable error) {
        List<Throwable> chain = new ArrayList<>();
        Throwable cursor = error;
        while (cursor != null) {
            chain.add(cursor);
            cursor = cursor.getCause();
        }
        return chain;
    }
}
