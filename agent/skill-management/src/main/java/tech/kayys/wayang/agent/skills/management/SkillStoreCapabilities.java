package tech.kayys.wayang.agent.skills.management;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable capability set for definition, lifecycle, and event stores.
 */
public record SkillStoreCapabilities(Set<SkillStoreCapability> values) {

    public SkillStoreCapabilities {
        values = immutableCopy(values);
    }

    public static SkillStoreCapabilities none() {
        return new SkillStoreCapabilities(Set.of());
    }

    public static SkillStoreCapabilities of(SkillStoreCapability... capabilities) {
        EnumSet<SkillStoreCapability> values = EnumSet.noneOf(SkillStoreCapability.class);
        if (capabilities != null) {
            Arrays.stream(capabilities)
                    .filter(Objects::nonNull)
                    .forEach(values::add);
        }
        return new SkillStoreCapabilities(values);
    }

    public static SkillStoreCapabilities definitionStore() {
        return of(
                SkillStoreCapability.READ,
                SkillStoreCapability.WRITE,
                SkillStoreCapability.DELETE,
                SkillStoreCapability.LIST);
    }

    public static SkillStoreCapabilities lifecycleStateStore() {
        return of(
                SkillStoreCapability.READ,
                SkillStoreCapability.WRITE,
                SkillStoreCapability.DELETE,
                SkillStoreCapability.LIST);
    }

    public static SkillStoreCapabilities eventStore(Object store) {
        SkillStoreCapabilities capabilities = none();
        if (store instanceof SkillManagementEventSink) {
            capabilities = capabilities.with(SkillStoreCapability.WRITE);
        }
        if (store instanceof SkillManagementEventReader) {
            capabilities = capabilities.with(SkillStoreCapability.QUERY_EVENTS);
        }
        if (store instanceof SkillManagementEventPruner pruner && pruner.supportsPruning()) {
            capabilities = capabilities.with(SkillStoreCapability.PRUNE_EVENTS);
        }
        if (store instanceof CompositeSkillManagementEventSink) {
            capabilities = capabilities.with(SkillStoreCapability.COMPOSITE);
        }
        if (store instanceof MirroredSkillManagementEventSink) {
            capabilities = capabilities
                    .with(SkillStoreCapability.PRIMARY_FALLBACK)
                    .with(SkillStoreCapability.MIRROR_WRITE);
        }
        return capabilities;
    }

    public boolean supports(SkillStoreCapability capability) {
        return values.contains(capability);
    }

    public SkillStoreCapabilities with(SkillStoreCapability capability) {
        if (capability == null || values.contains(capability)) {
            return this;
        }
        EnumSet<SkillStoreCapability> copy = EnumSet.noneOf(SkillStoreCapability.class);
        copy.addAll(values);
        copy.add(capability);
        return new SkillStoreCapabilities(copy);
    }

    public List<String> names() {
        return Arrays.stream(SkillStoreCapability.values())
                .filter(values::contains)
                .map(SkillStoreCapability::label)
                .toList();
    }

    private static Set<SkillStoreCapability> immutableCopy(Set<SkillStoreCapability> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        EnumSet<SkillStoreCapability> copy = EnumSet.noneOf(SkillStoreCapability.class);
        values.stream()
                .filter(Objects::nonNull)
                .forEach(copy::add);
        return copy.isEmpty() ? Set.of() : Collections.unmodifiableSet(copy);
    }
}
