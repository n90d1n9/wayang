/*
 * PolyForm Noncommercial License 1.0.0
 *
 * Copyright (c) 2026 Kayys.tech
 *
 * This software is licensed for non-commercial use only.
 * You may use, modify, and distribute this software for personal,
 * educational, or research purposes.
 *
 * Commercial use, including SaaS or revenue-generating services,
 * requires a separate commercial license from Kayys.tech.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 *
 * @author Bhangun
 */

package tech.kayys.wayang.plugin.spi.node;

import java.util.Map;

/**
 * Immutable definition of a workflow node contributed by a plugin module.
 *
 * <p>
 * Mirrors the {@link Node @Node} annotation fields but is available
 * programmatically for runtime discovery via {@link NodeProvider}.
 * </p>
 *
 * @param type          Unique node-type identifier, e.g. {@code "rag-executor"}
 * @param label         Human-readable display name
 * @param category      Top-level category for UI grouping (e.g. "AI", "Human")
 * @param subCategory   Optional sub-category
 * @param description   Short description of what the node does
 * @param icon          Icon identifier (default {@code "box"})
 * @param color         Hex color for UI rendering (default {@code "#6B7280"})
 * @param configSchema  JSON Schema string for the node's configuration
 * @param inputSchema   JSON Schema string for the node's input
 * @param outputSchema  JSON Schema string for the node's output
 * @param defaultConfig Default configuration values (may be empty)
 */
public record NodeDefinition(
        String type,
        String label,
        String category,
        String subCategory,
        String description,
        String icon,
        String color,
        String configSchema,
        String inputSchema,
        String outputSchema,
        Map<String, Object> defaultConfig) {

    /**
     * Compact constructor – fills in sensible defaults for optional fields.
     */
    public NodeDefinition {
        if (type == null || type.isBlank())
            throw new IllegalArgumentException("type must not be blank");
        if (label == null || label.isBlank())
            label = type;
        if (category == null)
            category = "";
        if (subCategory == null)
            subCategory = "";
        if (description == null)
            description = "";
        if (icon == null || icon.isBlank())
            icon = "box";
        if (color == null || color.isBlank())
            color = "#6B7280";
        if (configSchema == null)
            configSchema = "";
        if (inputSchema == null)
            inputSchema = "";
        if (outputSchema == null)
            outputSchema = "";
        if (defaultConfig == null)
            defaultConfig = Map.of();
    }
}
