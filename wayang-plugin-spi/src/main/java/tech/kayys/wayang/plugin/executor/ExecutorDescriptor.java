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

package tech.kayys.wayang.plugin.executor;

import java.net.URI;
import java.util.Set;

import tech.kayys.wayang.plugin.CommunicationProtocol;

/**
 * Executor Descriptor in contract
 */
public class ExecutorDescriptor {
    public String executorId;
    public Set<String> capabilities;
    public URI endpoint;
    public CommunicationProtocol protocol;
}