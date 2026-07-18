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

package tech.kayys.wayang.plugin.multi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import tech.kayys.wayang.plugin.execution.ExecutionMode;

/**
 * Marks an executor that handles multiple node types
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiNodeExecutor {

    /**
     * Executor ID
     */
    String executorId();

    /**
     * Node types this executor handles
     */
    String[] nodeTypes();

    /**
     * Execution mode
     */
    ExecutionMode mode() default ExecutionMode.SYNC;

    /**
     * Protocols
     */
    String[] protocols() default { "REST" };
}