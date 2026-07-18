/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry.node;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a single workflow node contributed by a plugin class.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Nodes.class)
public @interface Node {
    String type();
    String label() default "";
    String category() default "";
    String subCategory() default "";
    String description() default "";
    String configSchema() default "";
    String inputSchema() default "";
    String outputSchema() default "";
    String executorId() default "";
    String widgetId() default "";
}
