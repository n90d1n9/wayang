package tech.kayys.wayang.rag.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class RagPluginStrategyRegistry {

    private final RagPluginSelectionStrategy defaultStrategy;
    private final Map<String, RagPluginSelectionStrategy> strategiesById;

    private RagPluginStrategyRegistry(
            RagPluginSelectionStrategy defaultStrategy,
            Map<String, RagPluginSelectionStrategy> strategiesById) {
        this.defaultStrategy = Objects.requireNonNull(defaultStrategy, "defaultStrategy");
        this.strategiesById = Collections.unmodifiableMap(new LinkedHashMap<>(strategiesById));
    }

    static RagPluginStrategyRegistry from(
            Iterable<RagPluginSelectionStrategy> strategies,
            RagPluginSelectionStrategy defaultStrategy) {
        Objects.requireNonNull(defaultStrategy, "defaultStrategy");
        Map<String, RagPluginSelectionStrategy> indexed = new LinkedHashMap<>();
        if (strategies != null) {
            for (RagPluginSelectionStrategy strategy : strategies) {
                if (strategy == null || strategy.id() == null || strategy.id().isBlank()) {
                    continue;
                }
                indexed.put(RagPluginSelectionConfig.normalizeStrategyId(strategy.id()), strategy);
            }
        }
        indexed.putIfAbsent(RagPluginSelectionConfig.normalizeStrategyId(defaultStrategy.id()), defaultStrategy);
        return new RagPluginStrategyRegistry(defaultStrategy, indexed);
    }

    RagPluginSelectionStrategy defaultStrategy() {
        return defaultStrategy;
    }

    RagPluginSelectionStrategy strategyFor(String strategyId) {
        return strategiesById.getOrDefault(
                RagPluginSelectionConfig.normalizeStrategyId(strategyId),
                defaultStrategy);
    }

    boolean contains(String strategyId) {
        return strategiesById.containsKey(RagPluginSelectionConfig.normalizeStrategyId(strategyId));
    }

    List<String> strategyIds() {
        return strategiesById.keySet().stream().toList();
    }
}
