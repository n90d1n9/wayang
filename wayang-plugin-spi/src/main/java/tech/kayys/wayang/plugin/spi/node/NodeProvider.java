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

import java.util.List;

import tech.kayys.wayang.plugin.WayangPlugin;

/**
 * SPI for modules to contribute fully-described workflow nodes at runtime.
 *
 * <p>
 * Extends {@link WayangPlugin} so that every node provider has a compulsory
 * identity (id, name, version, description). This removes the need to
 * separately implement {@code WayangPlugin} when contributing nodes.
 * </p>
 *
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 * The orchestrator and schema catalog both consume this SPI to build
 * the unified node registry.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>
 * {@code
 * public class MyNodeProvider implements NodeProvider {
 *     &#64;Override public String id()          { return "com.example.my-plugin"; }
 *     &#64;Override public String name()        { return "My Plugin"; }
 *     &#64;Override public String version()     { return "1.0.0"; }
 *     &#64;Override public String description() { return "Custom node set."; }
 *
 *     @Override
 *     public List<NodeDefinition> nodes() {
 *         return List.of(
 *             new NodeDefinition("my-node", "My Node", "Custom", ...));
 *     }
 * }
 * }
 * </pre>
 *
 * <p>
 * For annotation-based node declaration (without implementing this interface),
 * use {@code @MultiNodePlugin} + {@code @Node} on the plugin class. Both paths
 * converge in the {@code ControlPlaneNodeRegistry}.
 * </p>
 */
public interface NodeProvider extends WayangPlugin {

    /**
     * @return an immutable list of node definitions contributed by this module.
     */
    List<NodeDefinition> nodes();
}
