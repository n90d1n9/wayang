package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered registry of JSON-RPC method handler groups.
 */
final class WayangA2aJsonRpcMethodHandlerRegistry {

    private final List<WayangA2aJsonRpcMethodHandlerGroup> groups;
    private final WayangA2aJsonRpcMethodHandlerRegistryAssembly assembly;
    private final WayangA2aJsonRpcMethodHandlerOverridePolicy overridePolicy;

    private WayangA2aJsonRpcMethodHandlerRegistry(
            List<WayangA2aJsonRpcMethodHandlerGroup> groups,
            WayangA2aJsonRpcMethodHandlerOverridePolicy overridePolicy) {
        this.groups = List.copyOf(groups);
        this.overridePolicy = Objects.requireNonNull(overridePolicy, "overridePolicy");
        this.assembly = WayangA2aJsonRpcMethodHandlerRegistryAssembly.from(this.groups, this.overridePolicy);
    }

    static Builder builder() {
        return new Builder();
    }

    List<WayangA2aJsonRpcMethodHandlerGroup> groups() {
        return groups;
    }

    List<String> groupNames() {
        return groups.stream()
                .map(WayangA2aJsonRpcMethodHandlerGroup::name)
                .toList();
    }

    Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers() {
        return assembly.handlers();
    }

    WayangA2aJsonRpcMethodHandlerOverridePolicy overridePolicy() {
        return overridePolicy;
    }

    List<WayangA2aJsonRpcMethodHandlerOverride> overrides() {
        return assembly.overrides();
    }

    List<Map<String, Object>> overrideMaps() {
        return assembly.overrideMaps();
    }

    static final class Builder {

        private final List<WayangA2aJsonRpcMethodHandlerGroup> groups = new ArrayList<>();
        private WayangA2aJsonRpcMethodHandlerOverridePolicy overridePolicy =
                WayangA2aJsonRpcMethodHandlerOverridePolicy.ALLOW_REPLACE;

        Builder add(WayangA2aJsonRpcMethodHandlerGroup group) {
            groups.add(Objects.requireNonNull(group, "group"));
            return this;
        }

        Builder overridePolicy(WayangA2aJsonRpcMethodHandlerOverridePolicy value) {
            overridePolicy = Objects.requireNonNull(value, "overridePolicy");
            return this;
        }

        Builder addProvider(WayangA2aJsonRpcMethodHandlerProvider provider) {
            return add(Objects.requireNonNull(provider, "provider").group());
        }

        Builder addAll(List<WayangA2aJsonRpcMethodHandlerGroup> values) {
            if (values != null) {
                values.forEach(this::add);
            }
            return this;
        }

        Builder addProviders(List<? extends WayangA2aJsonRpcMethodHandlerProvider> values) {
            if (values != null) {
                values.forEach(this::addProvider);
            }
            return this;
        }

        WayangA2aJsonRpcMethodHandlerRegistry build() {
            return new WayangA2aJsonRpcMethodHandlerRegistry(groups, overridePolicy);
        }
    }
}
