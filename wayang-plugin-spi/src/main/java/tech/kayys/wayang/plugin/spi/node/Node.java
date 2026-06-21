/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.spi.node;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a single workflow node contributed by a plugin class.
 * A class annotated with {@code @MultiNodePlugin}
 * may carry one or more {@code @Node} annotations to declare all the node types
 * it provides.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Nodes.class)
public @interface Node {

    /** Unique node-type identifier, e.g. {@code "rag-executor"}. */
    String type();

    /** Human-readable display label. Defaults to {@link #type()}. */
    String label() default "";

    /** Top-level category for UI grouping (e.g. "AI", "Human"). */
    String category() default "";

    /** Optional sub-category for finer-grained grouping. */
    String subCategory() default "";

    /** Short description of what this node does. */
    String description() default "";

    /** Resource path or inline JSON Schema for the node's configuration. */
    String configSchema() default "";

    /** Resource path or inline JSON Schema for the node's input. */
    String inputSchema() default "";

    /** Resource path or inline JSON Schema for the node's output. */
    String outputSchema() default "";

    /** Optional executor ID override; defaults to pluginId + ".executor". */
    String executorId() default "";

    /** Optional UI widget ID for the UI renderer. */
    String widgetId() default "";
}
